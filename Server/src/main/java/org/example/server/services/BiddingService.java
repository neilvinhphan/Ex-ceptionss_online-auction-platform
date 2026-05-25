package org.example.server.services;

import org.example.core.dto.Response;
import org.example.core.dto.bidDTO.BidBroadcastDTO;
import org.example.core.dto.bidDTO.BidRequestDTO;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dịch vụ xử lý lượt đặt thầu thủ công, kích hoạt thuật toán đấu giá tự động (Robot gác phòng) và
 * đồng bộ cơ chế chống bắn tỉa phút chót Anti-Sniping bằng hệ thống ReentrantLock phân mảnh.
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

  BiddingService(
      BidDAO bidDAO,
      AuctionDAO auctionDAO,
      WalletDAO walletDAO,
      UserDAO userDAO,
      AutoBidDAO autoBidDAO) {
    this.bidDAO = bidDAO;
    this.auctionDAO = auctionDAO;
    this.walletDAO = walletDAO;
    this.userDAO = userDAO;
    this.autoBidDAO = autoBidDAO;
  }

  /** Lấy instance duy nhất (Singleton) của BiddingService (Thread-safe). */
  public static BiddingService getInstance() {
    if (instance == null) {
      synchronized (BiddingService.class) {
        if (instance == null) {
          instance =
              new BiddingService(
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

  // --- NHÓM PHƯƠNG THỨC THAY ĐỔI DỮ LIỆU & ĐẶT GIÁ (WRITE LOGIC) ---

  /**
   * Tiếp nhận và xử lý an toàn luồng lượt trả giá thủ công của người dùng, kiểm tra điều kiện số dư
   * khả dụng, ghi nhận Database giao dịch dẫn đầu và kích hoạt nới lỏng Anti-Sniping.
   */
  public boolean placeBid(BidRequestDTO request) throws Exception {
    if (request == null) {
      throw new IllegalArgumentException("Yêu cầu đặt giá không hợp lệ hoặc để trống!");
    }
    if (request.getAuctionId() <= 0 || request.getUserId() <= 0) {
      throw new IllegalArgumentException(
          "Thông số định danh phòng hoặc tài khoản người đặt thầu không hợp lệ!");
    }
    if (request.getBidAmount() == null || request.getBidAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Số tiền đấu giá đặt lên phải lớn hơn 0 VNĐ!");
    }

    String threadName = Thread.currentThread().getName();
    ReentrantLock lock = getLock(request.getAuctionId());

    lock.lock();
    try {
      logger.info(
          "["
              + threadName
              + "] 🟢 Đã giữ khóa xác thực phân mảnh phòng "
              + request.getAuctionId()
              + " xử lý cho User ID: "
              + request.getUserId());
      LocalDateTime now = LocalDateTime.now();

      Auction auction = auctionDAO.getAuctionByAuctionId(request.getAuctionId());
      if (auction == null) {
        throw new Exception("Không tìm thấy dữ liệu của phiên đấu giá này trên hệ thống.");
      }

      BigDecimal currentPrice = bidDAO.getCurrentPrice(request.getAuctionId());
      if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
        currentPrice = auction.getItem().getStartingPrice();
      }

      Integer highestBidderId = auction.getBidderId();

      // Nếu người dùng tự nâng giá trần của chính họ khi đang dẫn đầu
      if (highestBidderId != null && highestBidderId == request.getUserId()) {
        if (request.getBidAmount().compareTo(currentPrice) <= 0) {
          throw new Exception(
              "Bạn đang là người dẫn đầu phòng! Số tiền đặt mới phải lớn hơn giá hiện tại: "
                  + currentPrice);
        }
      } else {
        auction.validateBid(now, request.getBidAmount());
      }

      BigDecimal bidIncrement = auctionDAO.getBidIncrementByAuctionId(request.getAuctionId());
      if (bidIncrement == null || bidIncrement.compareTo(BigDecimal.ZERO) <= 0) {
        bidIncrement = BigDecimal.ONE;
      }

      BigDecimal minAcceptable =
          (bidDAO.getCurrentPrice(request.getAuctionId()) == null)
              ? auction.getItem().getStartingPrice()
              : currentPrice.add(bidIncrement);

      if (request.getBidAmount().compareTo(minAcceptable) < 0) {
        throw new Exception(
            "Mức giá đặt thầu tối thiểu tiếp theo phải lớn hơn hoặc bằng: "
                + minAcceptable
                + " VNĐ");
      }

      if (request.getBidAmount().compareTo(walletDAO.getAvailableBalance(request.getUserId()))
          > 0) {
        throw new Exception(
            "Số dư khả dụng trong tài khoản ví của bạn không đủ để thực hiện lượt trả giá này!");
      }

      var user = userDAO.getUserByUserId(request.getUserId());
      if (user == null) {
        throw new Exception("Tài khoản người dùng đặt giá không tồn tại trên hệ thống!");
      }

      BidTransaction manualBidTx =
          new BidTransaction(request.getBidAmount(), now, request.getUserId(), user.getUserName());
      manualBidTx.setAuctionId(request.getAuctionId());

      if (!bidDAO.insertBid(manualBidTx)) {
        throw new Exception(
            "Lỗi hệ thống: Không thể ghi nhận lịch sử lượt đặt giá mới vào cơ sở dữ liệu.");
      }

      bidDAO.updateCurrentPrice(
          request.getAuctionId(), request.getUserId(), request.getBidAmount());
      handleAntiSniping(request.getAuctionId(), auction, now);

      logger.info(
          "["
              + threadName
              + "] 💾 Ghi nhận Database thành công lượt đặt thầu tay của User: "
              + request.getUserId());
      return true;
    } finally {
      lock.unlock();
      logger.info(
          "[" + threadName + "] 🔓 Đã giải phóng chìa khóa lock phòng " + request.getAuctionId());
    }
  }

  /**
   * Kích hoạt lõi tính toán toán học đấu giá tự động (Deterministic Game Engine). Phân loại thứ bậc
   * các cấu hình Robot đang chạy ngầm trong phòng để tự động đẩy bước giá cao nhất tức thời mà
   * không gây lặp vòng lặp.
   */
  public synchronized void evaluateDeterministicBidding(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) return;

      BigDecimal highestBid = bidDAO.getCurrentPrice(auctionId);
      BigDecimal currentPrice =
          (highestBid == null || highestBid.compareTo(BigDecimal.ZERO) <= 0)
              ? auction.getItem().getStartingPrice()
              : highestBid;

      BigDecimal increment = auction.getBidIncrement();
      List<AutoBidDAO.AutoBidConfig> activeBots = autoBidDAO.getActiveAutoBidsForAuction(auctionId);
      if (activeBots.isEmpty()) return;

      AutoBidDAO.AutoBidConfig bot1 = activeBots.get(0);

      if (activeBots.size() >= 2) {
        // Tách lọc phân hạng cấu hình Bot Trùm và Bot Nhì dựa trên mức giá trần gác phòng
        AutoBidDAO.AutoBidConfig highestBot =
            bot1.getMaxBid().compareTo(activeBots.get(1).getMaxBid()) >= 0
                ? bot1
                : activeBots.get(1);
        AutoBidDAO.AutoBidConfig secondBot =
            bot1.getMaxBid().compareTo(activeBots.get(1).getMaxBid()) >= 0
                ? activeBots.get(1)
                : bot1;

        if (highestBot.getMaxBid().compareTo(secondBot.getMaxBid()) == 0) {
          if (currentPrice.compareTo(highestBot.getMaxBid()) < 0) {
            BigDecimal finalPrice = highestBot.getMaxBid();
            int winnerId =
                highestBot.getCreatedAt().isBefore(secondBot.getCreatedAt())
                    ? highestBot.getUserId()
                    : secondBot.getUserId();

            if (finalPrice.compareTo(currentPrice) > 0 && auction.getBidderId() != winnerId) {
              executeAutoBidTransaction(auctionId, finalPrice, winnerId, auction);
            }
          }
        } else {
          BigDecimal baseComparePrice =
              secondBot.getMaxBid().compareTo(currentPrice) > 0
                  ? secondBot.getMaxBid()
                  : currentPrice;
          BigDecimal finalPrice = baseComparePrice.add(increment);

          if (finalPrice.compareTo(highestBot.getMaxBid()) > 0) {
            finalPrice = highestBot.getMaxBid();
          }

          if (finalPrice.compareTo(currentPrice) > 0
              && auction.getBidderId() != highestBot.getUserId()) {
            executeAutoBidTransaction(auctionId, finalPrice, highestBot.getUserId(), auction);
          }
        }
      } else {
        // Luồng xử lý độc quyền một Robot duy nhất tự động gác phòng nâng giá
        if (bot1.getMaxBid().compareTo(currentPrice) > 0
            && auction.getBidderId() != bot1.getUserId()) {
          BigDecimal finalPrice = currentPrice.add(increment);
          if (finalPrice.compareTo(bot1.getMaxBid()) > 0) {
            finalPrice = bot1.getMaxBid();
          }
          executeAutoBidTransaction(auctionId, finalPrice, bot1.getUserId(), auction);
        }
      }

      // TIẾN TRÌNH DỌN DẸP CHIẾN TRƯỜNG: Vô hiệu hóa đóng băng các Robot gác phòng đã chạm hạn mức
      // trần tối đa
      Auction checkAuction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (checkAuction != null) {
        BigDecimal latestDbPrice = bidDAO.getCurrentPrice(auctionId);
        BigDecimal latestPrice =
            (latestDbPrice != null && latestDbPrice.compareTo(BigDecimal.ZERO) > 0)
                ? latestDbPrice
                : checkAuction.getItem().getStartingPrice();

        int currentLeaderId = checkAuction.getBidderId();

        for (AutoBidDAO.AutoBidConfig bot : activeBots) {
          boolean isOutbid = bot.getMaxBid().compareTo(latestPrice) < 0;
          boolean isMaxedOutAndLost =
              (bot.getMaxBid().compareTo(latestPrice) == 0 && bot.getUserId() != currentLeaderId);

          if (isOutbid || isMaxedOutAndLost) {
            autoBidDAO.disableAutoBid(auctionId, bot.getUserId());
            logger.info(
                "🤖 [AUTOBID CLEANUP] Đã tự động tắt ngắt cấu hình Bot của User "
                    + bot.getUserId()
                    + " tại phòng "
                    + auctionId);

            AuctionServer.broadcastToRoom(
                auctionId, new Response("AUTOBID_DISABLED", "Bot chạm trần", bot.getUserId()));
          }
        }
      }
    } catch (Exception e) {
      logger.log(
          Level.SEVERE,
          "❌ Lỗi phát sinh trong bộ lõi tính toán đấu thầu tự động Đa luồng Robot Engine",
          e);
    }
  }

  /**
   * Đồng bộ lưu trữ thực thi lịch sử đặt thầu tự động từ AI Engine, nới rộng Anti-Sniping và phát
   * sóng Broadcast.
   */
  private void executeAutoBidTransaction(
      int auctionId, BigDecimal finalPrice, int winnerId, Auction auction) throws Exception {
    LocalDateTime now = LocalDateTime.now();
    String winnerName = userDAO.getUserByUserId(winnerId).getUserName();

    BidTransaction autoBidTx = new BidTransaction(finalPrice, now, winnerId, winnerName);
    autoBidTx.setAuctionId(auctionId);

    bidDAO.insertBid(autoBidTx);
    bidDAO.updateCurrentPrice(auctionId, winnerId, finalPrice);

    handleAntiSniping(auctionId, auction, now);

    BidBroadcastDTO broadcastData =
        new BidBroadcastDTO(
            auctionId, finalPrice.doubleValue(), winnerName, auction.getEndTime(), true);
    AuctionServer.broadcastToRoom(
        auctionId, new Response("NEW_BID", "Hệ thống tự động đẩy giá!", broadcastData));
    logger.info(
        "🚀 [AUTOBID TRANSACTION] Bot của "
            + winnerName
            + " tự động đẩy giá phòng "
            + auctionId
            + " lên "
            + finalPrice
            + " đ.");
  }

  /**
   * Cơ chế Anti-Sniping bảo vệ sàn: Tự động gia hạn kéo dài thêm 120 giây đếm ngược nếu phòng nhận
   * được lượt đặt giá hợp lệ sát thời điểm đóng cửa phòng đấu giá.
   */
  private void handleAntiSniping(int auctionId, Auction auction, LocalDateTime now) {
    LocalDateTime endTime = auction.getEndTime();
    if (endTime == null || !endTime.isAfter(now)) return;

    long antiSnipingSeconds = 120;
    if (!auction.isAntiSniping(now, antiSnipingSeconds)) return;

    auction.extendEndTime(antiSnipingSeconds);
    auctionDAO.updateAuctionEndTime(auctionId, auction.getEndTime());
    logger.info(
        "⏳ [ANTI-SNIPING ACTIVATED] Phát hiện thầu muộn phút chót. Phòng "
            + auctionId
            + " gia hạn thêm 120s. EndTime mới: "
            + auction.getEndTime());
  }

  public void saveOrUpdateAutoBid(int auctionId, int userId, BigDecimal maxBid) throws Exception {
    if (auctionId <= 0 || userId <= 0) {
      throw new IllegalArgumentException(
          "Mã phòng hoặc mã người dùng thiết lập Robot tự động trả giá không hợp lệ.");
    }
    if (maxBid == null || maxBid.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(
          "Mức giá trần thiết lập tối đa cho Robot phải lớn hơn 0 VNĐ.");
    }
    if (maxBid.compareTo(walletDAO.getAvailableBalance(userId)) > 0) {
      throw new Exception("Giá đặt trần tự động không được vượt quá số dư ví khả dụng hiện tại!");
    }
    autoBidDAO.saveOrUpdateAutoBid(auctionId, userId, maxBid);
    logger.info(
        "🤖 Cài đặt cấu hình đặt giá tự động AutoBid thành công cho User ID: "
            + userId
            + " tại phòng "
            + auctionId);
  }

  public void disableAutoBid(int auctionId, int userId) throws Exception {
    if (auctionId <= 0 || userId <= 0) {
      throw new IllegalArgumentException(
          "Mã định danh không hợp lệ để hủy trạng thái đấu giá tự động.");
    }
    autoBidDAO.disableAutoBid(auctionId, userId);
    logger.info(
        "🤖 Người dùng ID " + userId + " chủ động hủy bỏ cấu hình Bot gác phòng " + auctionId);
  }

  // --- NHÓM PHƯƠNG THỨC TRUY VẤN DỮ LIỆU (READ LOGIC) ---

  public BigDecimal getMaxAutoBid(int auctionId, int userId) throws Exception {
    if (auctionId <= 0 || userId <= 0) {
      throw new IllegalArgumentException(
          "Thông tin định danh phòng đấu giá hoặc người dùng bị sai.");
    }
    return autoBidDAO.getMaxAutoBid(auctionId, userId);
  }

  public List<BidTransaction> getBidHistory(int auctionId) throws Exception {
    if (auctionId <= 0) {
      throw new IllegalArgumentException("Mã phiên đấu giá cần lấy lịch sử biểu đồ không hợp lệ!");
    }
    return bidDAO.getBidHistoryByAuctionId(auctionId);
  }
}
