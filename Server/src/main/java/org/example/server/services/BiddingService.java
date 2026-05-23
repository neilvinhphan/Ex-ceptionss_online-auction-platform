package org.example.server.services;

import org.example.core.dto.bidDTO.BidRequestDTO;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.server.daos.AuctionDAO;
import org.example.server.daos.BidDAO;
import org.example.server.daos.UserDAO;
import org.example.server.daos.WalletDAO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class BiddingService {

  private static volatile BiddingService instance;

  private static final BidDAO bidDAO = BidDAO.getInstance();
  private static final AuctionDAO auctionDAO = AuctionDAO.getInstance();
  private static final WalletDAO walletDAO = WalletDAO.getInstance();
  private static final UserDAO userDAO = UserDAO.getInstance();

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

  public boolean placeBid(BidRequestDTO request) throws Exception {
    String threadName = Thread.currentThread().getName();

    ReentrantLock lock = getLock(request.getAuctionId());
    lock.lock();
    try {
      System.out.println("[" + threadName + "] 🟢 Đã cầm chìa khóa (Lock)! Đang xử lý giá cho User " + request.getUserId());

      LocalDateTime now = LocalDateTime.now();

      Auction auction = auctionDAO.getAuctionByAuctionId(request.getAuctionId());
      if (auction == null) {
        throw new Exception("Không tìm thấy phiên đấu giá.");
      }

      // 1. Tải giá thầu cao nhất hiện tại từ bảng giao dịch
      BigDecimal currentPrice = bidDAO.getCurrentPrice(request.getAuctionId());
      if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
        currentPrice = auction.getItem().getStartingPrice(); // Nếu chưa có ai đặt, lấy giá khởi điểm thực tế
      }

      // --- 🔥 FIX LỖI 2: NỚI LỎNG BIẾN VALIDATE CHẶN CHÍNH MÌNH ---
      // Kiểm tra xem người đặt hiện tại có phải người dẫn đầu không
      Integer highestBidderId = auction.getBidderId(); // Lấy ID người dẫn đầu phòng hiện tại

      // Nếu chính mình đang dẫn đầu, nhưng số tiền muốn đặt thủ công lớn hơn giá hiện tại -> Hợp lệ!
      if (highestBidderId != null && highestBidderId == request.getUserId()) {
        if (request.getBidAmount().compareTo(currentPrice) <= 0) {
          throw new Exception("Bạn đang là người dẫn đầu phòng! Số tiền đặt mới phải lớn hơn giá hiện tại: " + currentPrice);
        }
      } else {
        // Nếu là người khác đặt, bọc hàm kiểm tra trạng thái thời gian phòng mặc định của ông
        auction.validateBid(now, request.getBidAmount());
      }

      // 2. Tính toán bước giá tối thiểu tiếp theo
      BigDecimal bidIncrement = auctionDAO.getBidIncrementByAuctionId(request.getAuctionId());
      if (bidIncrement == null || bidIncrement.compareTo(BigDecimal.ZERO) <= 0) {
        bidIncrement = BigDecimal.ONE;
      }

      // Xác định mức giá chấp nhận tối thiểu
      BigDecimal minAcceptable;
      BigDecimal checkDbPrice = bidDAO.getCurrentPrice(request.getAuctionId());
      if (checkDbPrice == null || checkDbPrice.compareTo(BigDecimal.ZERO) == 0) {
        // Nếu phòng trống trơn bóc tem: Người đầu tiên đặt bằng đúng giá khởi điểm là hợp lệ!
        minAcceptable = auction.getItem().getStartingPrice();
      } else {
        // Từ lượt thứ 2 trở đi bắt buộc phải >= Giá hiện tại + Bước giá
        minAcceptable = currentPrice.add(bidIncrement);
      }

      if (request.getBidAmount().compareTo(minAcceptable) < 0) {
        throw new Exception("Giá đặt phải lớn hơn hoặc bằng mức tối thiểu quy định: " + minAcceptable);
      }

      // 3. Kiểm tra số dư ví
      BigDecimal availableBalance = walletDAO.getAvailableBalance(request.getUserId());
      if (request.getBidAmount().compareTo(availableBalance) > 0) {
        throw new Exception("Số dư khả dụng trong ví không đủ để thực hiện đặt giá!");
      }

      // 4. Đồng bộ ghi nhận giao dịch đặt thầu
      String bidderName = userDAO.getUserByUserId(request.getUserId()).getUserName();
      BidTransaction manualBidTx = new BidTransaction(request.getBidAmount(), now, request.getUserId(), bidderName);
      manualBidTx.setAuctionId(request.getAuctionId());

      boolean inserted = bidDAO.insertBid(manualBidTx);
      if (!inserted) {
        throw new Exception("Không thể ghi nhận lượt đặt giá xuống cơ sở dữ liệu.");
      }

      // 🔥 ĐỒNG BỘ ĐỒNG THỜI CẢ 2 BẢNG ĐỂ TRÁNH LỆCH PHA DỮ LIỆU
      bidDAO.updateCurrentPrice(request.getAuctionId(), request.getUserId(), request.getBidAmount());
      auctionDAO.updateHighestPriceByItemId(request.getUserId(), request.getBidAmount());

      handleAntiSniping(request.getAuctionId(), auction, now);

      System.out.println("[" + threadName + "] 💾 Ghi DB thành công cho User " + request.getUserId() + " với mức giá: " + request.getBidAmount());

      return true;
    } catch (Exception e) {
      System.out.println("[" + threadName + "] ❌ Bị lỗi: " + e.getMessage());
      throw e;
    } finally {
      System.out.println("[" + threadName + "] 🔓 Trả lại chìa khóa (Unlock)!");
      lock.unlock();
    }
  }

  private void handleAntiSniping(int auctionId, Auction auction, LocalDateTime now) {
    LocalDateTime endTime = auction.getEndTime();
    if (endTime == null || !endTime.isAfter(now)) {
      return;
    }
    long antiSnipingSeconds = 120;
    boolean inSnipingWindow = auction.isAntiSniping(now, antiSnipingSeconds);
    if (!inSnipingWindow) return;
    auction.extendEndTime(antiSnipingSeconds);
    auctionDAO.updateAuctionEndTime(auctionId, auction.getEndTime());
  }

  public List<BidTransaction> getBidHistory(int auctionId) {
    return bidDAO.getBidHistoryByAuctionId(auctionId);
  }

  // 🔥 REFACTOR LẠI TOÀN BỘ LOGIC ĐỊNH GIÁ TOÁN HỌC CỦA AUTOBID KHÔNG BỊ SẬP GIÁ
  public static synchronized void evaluateDeterministicBidding(int auctionId) {
    try {
      org.example.core.models.entities.Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction == null || auction.getStatus() != org.example.core.shared.enums.AuctionStatus.RUNNING) return;

      // --- 🔥 FIX LỖI 1: TÍNH TOÁN GIÁ HIỆN TẠI CHUẨN XÁC ---
      BigDecimal dbPrice = bidDAO.getCurrentPrice(auctionId);
      BigDecimal startingPrice = auction.getItem().getStartingPrice();
      boolean isFirstBid = (dbPrice == null || dbPrice.compareTo(BigDecimal.ZERO) == 0);

      BigDecimal currentPrice = isFirstBid ? startingPrice : dbPrice;
      BigDecimal increment = auction.getBidIncrement();

      List<org.example.server.daos.AutoBidDAO.AutoBidConfig> activeBots =
              org.example.server.daos.AutoBidDAO.getInstance().getActiveAutoBidsForAuction(auctionId);

      if (activeBots.isEmpty()) return;

      org.example.server.daos.AutoBidDAO.AutoBidConfig bot1 = activeBots.get(0); // Bot trần cao nhất

      // =========================================================================
      // KỊCH BẢN A: CÓ TỪ 2 BOT TRỞ LÊN ĐỤNG ĐỘ SÁT PHẠT NHAU
      // =========================================================================
      if (activeBots.size() >= 2) {
        org.example.server.daos.AutoBidDAO.AutoBidConfig bot2 = activeBots.get(1); // Bot trần cao thứ nhì

        if (bot1.getMaxBid().compareTo(bot2.getMaxBid()) == 0) {
          if (currentPrice.compareTo(bot1.getMaxBid()) < 0) {
            BigDecimal finalPrice = bot1.getMaxBid();
            int winnerId = bot1.getCreatedAt().isBefore(bot2.getCreatedAt()) ? bot1.getUserId() : bot2.getUserId();

            // Chỉ kích hoạt nếu giá mới thực sự đẩy căn phòng lên cao hơn hoặc đổi ngôi dẫn đầu
            if (isFirstBid || finalPrice.compareTo(dbPrice) > 0 || auction.getBidderId() != winnerId) {
              executeAutoBidTransaction(auctionId, finalPrice, winnerId, auction);
            }
          }
        }
        else if (bot1.getMaxBid().compareTo(bot2.getMaxBid()) > 0) {
          // Công thức: Trần thằng thua + 1 Bước giá thầu
          BigDecimal finalPrice = bot2.getMaxBid().add(increment);

          if (finalPrice.compareTo(bot1.getMaxBid()) > 0) {
            finalPrice = bot1.getMaxBid();
          }

          if (isFirstBid || finalPrice.compareTo(currentPrice) > 0 || auction.getBidderId() != bot1.getUserId()) {
            executeAutoBidTransaction(auctionId, finalPrice, bot1.getUserId(), auction);
          }
        }
      }
      // =========================================================================
      // KỊCH BẢN B: CHỈ CÓ DUY NHẤT 1 BOT CÔ ĐƠN GÁC PHÒNG
      // =========================================================================
      else {
        // Kiểm tra điều kiện:
        // Nếu là lượt đầu tiên: Thằng Bot tự động thầu bằng đúng Giá khởi điểm!
        // Nếu là lượt tiếp theo: Giá hiện tại + Bước giá (nếu chủ Bot chưa phải người dẫn đầu)
        if (isFirstBid) {
          if (bot1.getMaxBid().compareTo(startingPrice) >= 0) {
            executeAutoBidTransaction(auctionId, startingPrice, bot1.getUserId(), auction);
          }
        } else {
          if (bot1.getMaxBid().compareTo(currentPrice) > 0 && auction.getBidderId() != bot1.getUserId()) {
            BigDecimal finalPrice = currentPrice.add(increment);
            if (finalPrice.compareTo(bot1.getMaxBid()) > 0) {
              finalPrice = bot1.getMaxBid();
            }
            executeAutoBidTransaction(auctionId, finalPrice, bot1.getUserId(), auction);
          }
        }
      }
    } catch (Exception e) {
      System.err.println("❌ Lỗi xử lý lõi toán học AutoBid: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void executeAutoBidTransaction(int auctionId, java.math.BigDecimal finalPrice, int winnerId, org.example.core.models.entities.Auction auction) throws Exception {
    String winnerName = userDAO.getUserByUserId(winnerId).getUserName();

    BidTransaction autoBidTx = new BidTransaction(finalPrice, java.time.LocalDateTime.now(), winnerId, winnerName);
    autoBidTx.setAuctionId(auctionId);

    // Ghi nhận đồng bộ xuống DB giống hệt đặt tay để đồng nhất bộ đếm giá
    bidDAO.insertBid(autoBidTx);
    bidDAO.updateCurrentPrice(auctionId, winnerId, finalPrice); // 🔥 BỔ SUNG: Cập nhật đồng thời bảng giá hiện tại
    auctionDAO.updateHighestPriceByItemId(winnerId, finalPrice);

    // Cập nhật trạng thái trực tiếp vào thực thể memory để luồng socket nhận diện tức thì
    auction.setHighestBid(finalPrice);
    auction.setBidderId(winnerId);

    org.example.core.dto.bidDTO.BidBroadcastDTO broadcastData = new org.example.core.dto.bidDTO.BidBroadcastDTO(
            auctionId,
            finalPrice.doubleValue(),
            winnerName,
            auction.getEndTime(),
            true
    );

    org.example.core.dto.Response broadcastResponse = new org.example.core.dto.Response("NEW_BID", "Hệ thống tự động đẩy giá!", broadcastData);
    org.example.server.network.AuctionServer.broadcastToRoom(auctionId, broadcastResponse);
    System.out.println("🚀 [AUTOBID BROADCAST] Giá phòng " + auctionId + " tự động tăng vọt lên " + finalPrice + " đ bởi Bot của " + winnerName);
  }
}
