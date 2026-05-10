package org.example.server.services;

import org.example.core.dto.BidRequestDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.shared.enums.WalletTransactionType;
import org.example.server.daos.AuctionDAO;
import org.example.server.daos.BidDAO;
import org.example.server.daos.WalletDAO;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static org.example.server.network.ClientHandler.broadcastMessage;

public class BiddingService {

  private static volatile BiddingService instance;

  private static final BidDAO bidDAO = BidDAO.getInstance();
  private static final AuctionDAO auctionDAO = AuctionDAO.getInstance();
  private static final WalletDAO walletDAO = WalletDAO.getInstance();

  // lock theo từng auction để tránh đè giá cùng lúc
  private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

  public static BiddingService getInstance() {
    if (instance == null) {
      synchronized (BiddingService.class) {
        if (instance == null) {
          instance = new BiddingService();
        }
      }
    }
    return instance;
  }

  private ReentrantLock getLock(int auctionId) {
    return auctionLocks.computeIfAbsent(auctionId, id -> new ReentrantLock());
  }

  /**
   * z placeBid (cốt lõi): 1) lock theo auction 2) validate domain 3) validate amount >= current +
   * increment 4) ghi DB bid + cập nhật current price 5) anti sniping
   */
  public boolean placeBid(BidRequestDTO request) throws Exception {
    ReentrantLock lock = getLock(request.getAuctionId());
    lock.lock();
    try {
      LocalDateTime now = LocalDateTime.now();

      Auction auction = auctionDAO.getAuctionByAuctionId(request.getAuctionId());
      if (auction == null) {
        throw new Exception("Không tìm thấy phiên đấu giá.");
      }
      // check trạng thái + thời gian (đang có sẵn trong domain Auction)
      auction.validateBid(now, request.getBidAmount());

      BigDecimal currentPrice = bidDAO.getCurrentPrice(request.getAuctionId());
      if (currentPrice == null) {
        currentPrice = BigDecimal.ZERO;
      }

      BigDecimal bidIncrement = auctionDAO.getBidIncrementByAuctionId(request.getAuctionId());
      if (bidIncrement == null || bidIncrement.compareTo(BigDecimal.ZERO) <= 0) {
        bidIncrement = BigDecimal.ONE;
      }

      BigDecimal minAcceptable = currentPrice.add(bidIncrement);
      if (request.getBidAmount().compareTo(minAcceptable) < 0) {
        throw new Exception("Giá đặt phải >= " + minAcceptable);
      }

      BigDecimal availableBalance = walletDAO.getAvailableBalance(request.getUserId());
      if (request.getBidAmount().compareTo(availableBalance) > 0) {
        throw new Exception("Sô dư khả dụng không đủ!");
      }

      boolean inserted =
          bidDAO.updateNewBid(request.getAuctionId(), request.getUserId(), request.getBidAmount());
      if (!inserted) {
        throw new Exception("Không thể ghi nhận lượt đặt giá.");
      }

      boolean updatedPrice =
          bidDAO.updateCurrentPrice(
              request.getAuctionId(), request.getUserId(), request.getBidAmount());
      if (!updatedPrice) {
        throw new Exception("Không thể cập nhật giá hiện tại.");
      }

      handleAntiSniping(request.getAuctionId(), auction, now);

      return true;
    } finally {
      lock.unlock();
    }
  }

  /** handleAntiSniping: nếu bid trong 1/10 thời gian cuối thì cộng thêm 1/10 thời lượng phiên */
  private void handleAntiSniping(int auctionId, Auction auction, LocalDateTime now) {
    LocalDateTime endTime = auction.getEndTime();
    if (endTime == null || !endTime.isAfter(now)) {
      return; // Nếu chưa setup giờ hoặc phiên đã kết thúc thì bỏ qua
    }

    long antiSnipingSeconds = 120;

    boolean inSnipingWindow = auction.isAntiSniping(now, antiSnipingSeconds);
    if (!inSnipingWindow) return;
    auction.extendEndTime(antiSnipingSeconds);
    auctionDAO.updateAuctionEndTime(auctionId, auction.getEndTime());
  }

  /** Lấy lịch sử để FE vẽ chart */
  public List<BidTransaction> getBidHistory(int auctionId) {
    return bidDAO.getBidHistoryByAuctionId(auctionId);
  }
}
