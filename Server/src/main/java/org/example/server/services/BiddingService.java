package org.example.server.services;

import org.example.core.dto.Response;
import org.example.core.dto.bidDTO.BidBroadcastDTO;
import org.example.core.dto.bidDTO.BidRequestDTO;
import org.example.core.exception.DataConflictException;
import org.example.core.exception.InsufficientBalanceException;
import org.example.core.exception.InvalidUserDataException;
import org.example.core.exception.InvalidBidException;
import org.example.core.exception.ResourceNotFoundException;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.shared.enums.AuctionStatus;
import org.example.server.daos.AuctionDAO;
import org.example.server.daos.AutoBidDAO;
import org.example.server.daos.BidDAO;
import org.example.server.daos.UserDAO;
import org.example.server.daos.WalletDAO;
import org.example.server.network.AuctionServer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dịch vụ xử lý lượt đặt thầu thủ công và tự động.
 */
public class BiddingService {
  private static final Logger logger = Logger.getLogger(BiddingService.class.getName());
  private static volatile BiddingService instance;

  private final BidDAO bidDAO;
  private final AuctionDAO auctionDAO;
  private final WalletDAO walletDAO;
  private final UserDAO userDAO;
  private final AutoBidDAO autoBidDAO;
  private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

  BiddingService(BidDAO bidDAO, AuctionDAO auctionDAO, WalletDAO walletDAO, UserDAO userDAO, AutoBidDAO autoBidDAO) {
    this.bidDAO = bidDAO;
    this.auctionDAO = auctionDAO;
    this.walletDAO = walletDAO;
    this.userDAO = userDAO;
    this.autoBidDAO = autoBidDAO;
  }

  public static BiddingService getInstance() {
    if (instance == null) {
      synchronized (BiddingService.class) {
        if (instance == null) {
          instance = new BiddingService(
                  BidDAO.getInstance(),
                  AuctionDAO.getInstance(),
                  WalletDAO.getInstance(),
                  UserDAO.getInstance(),
                  AutoBidDAO.getInstance());
        }
      }
    }
    return instance;
  }

  private ReentrantLock getLock(int auctionId) {
    return auctionLocks.computeIfAbsent(auctionId, id -> new ReentrantLock());
  }

  public boolean placeBid(BidRequestDTO request) {
    if (request == null) {
      throw new InvalidUserDataException("Yêu cầu đặt giá không hợp lệ hoặc để trống!");
    }
    if (request.getAuctionId() <= 0 || request.getUserId() <= 0) {
      throw new InvalidUserDataException("Thông số định danh phòng hoặc tài khoản không hợp lệ!");
    }
    if (request.getBidAmount() == null || request.getBidAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidUserDataException("Số tiền đấu giá đặt lên phải lớn hơn 0 VNĐ!");
    }

    String threadName = Thread.currentThread().getName();
    ReentrantLock lock = getLock(request.getAuctionId());

    lock.lock();
    try {
      logger.info("[" + threadName + "] Đã giữ khóa phân mảnh phòng " + request.getAuctionId());
      LocalDateTime now = LocalDateTime.now();

      Auction auction = auctionDAO.getAuctionByAuctionId(request.getAuctionId());
      if (auction == null) {
        throw new ResourceNotFoundException("Không tìm thấy dữ liệu của phiên đấu giá này.");
      }

      BigDecimal currentPrice = bidDAO.getCurrentPrice(request.getAuctionId());
      if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
        currentPrice = auction.getItem().getStartingPrice();
      }

      Integer highestBidderId = auction.getBidderId();

      if (highestBidderId != null && highestBidderId == request.getUserId()) {
        if (request.getBidAmount().compareTo(currentPrice) <= 0) {
          throw new InvalidBidException("Bạn đang dẫn đầu phòng! Giá đặt mới phải lớn hơn giá hiện tại: " + currentPrice);
        }
      } else {
        try {
          auction.validateBid(now, request.getBidAmount());
        } catch (Exception e) {
          throw new InvalidBidException(e.getMessage());
        }
      }

      BigDecimal bidIncrement = auctionDAO.getBidIncrementByAuctionId(request.getAuctionId());
      if (bidIncrement == null || bidIncrement.compareTo(BigDecimal.ZERO) <= 0) {
        bidIncrement = BigDecimal.ONE;
      }

      BigDecimal minAcceptable = (bidDAO.getCurrentPrice(request.getAuctionId()) == null)
              ? auction.getItem().getStartingPrice()
              : currentPrice.add(bidIncrement);

      if (request.getBidAmount().compareTo(minAcceptable) < 0) {
        throw new InvalidBidException("Mức giá đặt thầu tối thiểu tiếp theo phải lớn hơn hoặc bằng: " + minAcceptable + " VNĐ");
      }

      if (request.getBidAmount().compareTo(walletDAO.getAvailableBalance(request.getUserId())) > 0) {
        throw new InsufficientBalanceException("Số dư khả dụng trong tài khoản ví không đủ thực hiện lượt trả giá!");
      }

      var user = userDAO.getUserByUserId(request.getUserId());
      BidTransaction manualBidTx = new BidTransaction(request.getBidAmount(), now, request.getUserId(), user.getUserName());
      manualBidTx.setAuctionId(request.getAuctionId());

      bidDAO.insertBid(manualBidTx);
      bidDAO.updateCurrentPrice(request.getAuctionId(), request.getUserId(), request.getBidAmount());
      handleAntiSniping(request.getAuctionId(), auction, now);

      return true;
    } finally {
      lock.unlock();
      logger.info("[" + threadName + "] 🔓 Đã giải phóng chìa khóa lock phòng " + request.getAuctionId());
    }
  }

  public synchronized void evaluateDeterministicBidding(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) return;

      BigDecimal highestBid = bidDAO.getCurrentPrice(auctionId);
      BigDecimal currentPrice = (highestBid == null || highestBid.compareTo(BigDecimal.ZERO) <= 0)
              ? auction.getItem().getStartingPrice() : highestBid;

      BigDecimal increment = auction.getBidIncrement();
      List<AutoBidDAO.AutoBidConfig> activeBots = autoBidDAO.getActiveAutoBidsForAuction(auctionId);
      if (activeBots.isEmpty()) return;

      AutoBidDAO.AutoBidConfig bot1 = activeBots.get(0);

      if (activeBots.size() >= 2) {
        AutoBidDAO.AutoBidConfig highestBot = bot1.getMaxBid().compareTo(activeBots.get(1).getMaxBid()) >= 0 ? bot1 : activeBots.get(1);
        AutoBidDAO.AutoBidConfig secondBot = bot1.getMaxBid().compareTo(activeBots.get(1).getMaxBid()) >= 0 ? activeBots.get(1) : bot1;

        if (highestBot.getMaxBid().compareTo(secondBot.getMaxBid()) == 0) {
          if (currentPrice.compareTo(highestBot.getMaxBid()) < 0) {
            BigDecimal finalPrice = highestBot.getMaxBid();
            int winnerId = highestBot.getCreatedAt().isBefore(secondBot.getCreatedAt()) ? highestBot.getUserId() : secondBot.getUserId();
            if (finalPrice.compareTo(currentPrice) > 0 && auction.getBidderId() != winnerId) {
              executeAutoBidTransaction(auctionId, finalPrice, winnerId, auction);
            }
          }
        } else {
          BigDecimal baseComparePrice = secondBot.getMaxBid().compareTo(currentPrice) > 0 ? secondBot.getMaxBid() : currentPrice;
          BigDecimal finalPrice = baseComparePrice.add(increment);
          if (finalPrice.compareTo(highestBot.getMaxBid()) > 0) finalPrice = highestBot.getMaxBid();
          if (finalPrice.compareTo(currentPrice) > 0 && auction.getBidderId() != highestBot.getUserId()) {
            executeAutoBidTransaction(auctionId, finalPrice, highestBot.getUserId(), auction);
          }
        }
      } else {
        if (bot1.getMaxBid().compareTo(currentPrice) > 0 && auction.getBidderId() != bot1.getUserId()) {
          BigDecimal finalPrice = currentPrice.add(increment);
          if (finalPrice.compareTo(bot1.getMaxBid()) > 0) finalPrice = bot1.getMaxBid();
          executeAutoBidTransaction(auctionId, finalPrice, bot1.getUserId(), auction);
        }
      }

      Auction checkAuction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (checkAuction != null) {
        BigDecimal latestDbPrice = bidDAO.getCurrentPrice(auctionId);
        BigDecimal latestPrice = (latestDbPrice != null && latestDbPrice.compareTo(BigDecimal.ZERO) > 0) ? latestDbPrice : checkAuction.getItem().getStartingPrice();
        int currentLeaderId = checkAuction.getBidderId();

        for (AutoBidDAO.AutoBidConfig bot : activeBots) {
          boolean isOutbid = bot.getMaxBid().compareTo(latestPrice) < 0;
          boolean isMaxedOutAndLost = (bot.getMaxBid().compareTo(latestPrice) == 0 && bot.getUserId() != currentLeaderId);
          if (isOutbid || isMaxedOutAndLost) {
            autoBidDAO.disableAutoBid(auctionId, bot.getUserId());
            AuctionServer.broadcastToRoom(auctionId, new Response("AUTOBID_DISABLED", "Bot chạm trần", bot.getUserId()));
          }
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi bộ lõi tính toán đấu thầu tự động", e);
    }
  }

  private void executeAutoBidTransaction(int auctionId, BigDecimal finalPrice, int winnerId, Auction auction) throws Exception {
    LocalDateTime now = LocalDateTime.now();
    String winnerName = userDAO.getUserByUserId(winnerId).getUserName();

    BidTransaction autoBidTx = new BidTransaction(finalPrice, now, winnerId, winnerName);
    autoBidTx.setAuctionId(auctionId);

    bidDAO.insertBid(autoBidTx);
    bidDAO.updateCurrentPrice(auctionId, winnerId, finalPrice);
    handleAntiSniping(auctionId, auction, now);

    BidBroadcastDTO broadcastData = new BidBroadcastDTO(auctionId, finalPrice.doubleValue(), winnerName, auction.getEndTime(), true);
    AuctionServer.broadcastToRoom(auctionId, new Response("NEW_BID", "Hệ thống tự động đẩy giá!", broadcastData));
  }

  private void handleAntiSniping(int auctionId, Auction auction, LocalDateTime now) {
    LocalDateTime endTime = auction.getEndTime();
    if (endTime == null || !endTime.isAfter(now)) return;

    long antiSnipingSeconds = 120;
    if (!auction.isAntiSniping(now, antiSnipingSeconds)) return;

    auction.extendEndTime(antiSnipingSeconds);
    auctionDAO.updateAuctionEndTime(auctionId, auction.getEndTime());
    logger.info("[ANTI-SNIPING] Phòng " + auctionId + " gia hạn đến: " + auction.getEndTime());
  }

  public void saveOrUpdateAutoBid(int auctionId, int userId, BigDecimal maxBid) {
    if (auctionId <= 0 || userId <= 0) {
      throw new InvalidUserDataException("Mã phòng hoặc người dùng thiết lập AutoBid không hợp lệ.");
    }
    if (maxBid == null || maxBid.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidUserDataException("Mức giá trần thiết lập tối đa phải lớn hơn 0 VNĐ.");
    }
    if (maxBid.compareTo(walletDAO.getAvailableBalance(userId)) > 0) {
      throw new InsufficientBalanceException("Giá đặt trần tự động không được vượt quá số dư ví khả dụng!");
    }
    autoBidDAO.saveOrUpdateAutoBid(auctionId, userId, maxBid);
  }

  public void disableAutoBid(int auctionId, int userId) {
    if (auctionId <= 0 || userId <= 0) {
      throw new InvalidUserDataException("Mã định danh không hợp lệ để hủy AutoBid.");
    }
    autoBidDAO.disableAutoBid(auctionId, userId);
  }

  public BigDecimal getMaxAutoBid(int auctionId, int userId) {
    if (auctionId <= 0 || userId <= 0) {
      throw new InvalidUserDataException("Thông tin định danh phòng đấu giá hoặc người dùng bị sai.");
    }
    return autoBidDAO.getMaxAutoBid(auctionId, userId);
  }

  public List<BidTransaction> getBidHistory(int auctionId) {
    if (auctionId <= 0) {
      throw new InvalidUserDataException("Mã phiên đấu giá cần lấy lịch sử biểu đồ không hợp lệ!");
    }
    return bidDAO.getBidHistoryByAuctionId(auctionId);
  }
}