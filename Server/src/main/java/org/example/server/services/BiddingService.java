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
    // TEST---
    String threadName = Thread.currentThread().getName();
    // ----

    ReentrantLock lock = getLock(request.getAuctionId());
    lock.lock();
    try {
      // TEST---
      System.out.println(
              "["
                      + threadName
                      + "] 🟢 Đã cầm chìa khóa (Lock)! Đang xử lý giá cho User "
                      + request.getUserId());
      // ----

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

      // 🔥 THAY ĐỔI CỐT LÕI: Đóng gói đối tượng BidTransaction trước khi nạp xuống DAO
      // 1. Lấy tên hiển thị của User đặt thầu tay từ cơ sở dữ liệu
      String bidderName = userDAO.getUserByUserId(request.getUserId()).getUserName();

      // 2. Khởi tạo thực thể theo constructor 4 tham số có sẵn trong Core
      BidTransaction manualBidTx = new BidTransaction(request.getBidAmount(), now, request.getUserId(), bidderName);
      manualBidTx.setAuctionId(request.getAuctionId()); // Gán mã phòng thông qua setter

      // 3. Truyền trọn gói thực thể xuống hàm insertBid mới của DAO
      boolean inserted = bidDAO.insertBid(manualBidTx);
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

      // TEST
      System.out.println(
              "[" + threadName + "] 💾 Ghi DB thành công cho User " + request.getUserId());
      // ----

      return true;
    } catch (Exception e) {
      // TEST
      System.out.println("[" + threadName + "] ❌ Bị lỗi: " + e.getMessage());
      throw e;
    } finally {
      // TEST
      System.out.println("[" + threadName + "] 🔓 Trả lại chìa khóa (Unlock)!");
      // ----
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

  // BỔ SUNG THÊM HÀM NÀY VÀO TRONG CLASS BiddingService.java
  public static synchronized void evaluateDeterministicBidding(int auctionId) {
    try {
      // 1. Tải thông tin thực tế của phòng đấu giá lên
      org.example.core.models.entities.Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction == null || auction.getStatus() != org.example.core.shared.enums.AuctionStatus.RUNNING) return;

      java.math.BigDecimal currentPrice = auction.getHighestBid() != null ? auction.getHighestBid() : auction.getItem().getStartingPrice();
      java.math.BigDecimal increment = auction.getBidIncrement();

      // 2. Tải tất cả Bot đang hoạt động (Sắp xếp: trần cao nhất lên đầu)
      List<org.example.server.daos.AutoBidDAO.AutoBidConfig> activeBots =
              org.example.server.daos.AutoBidDAO.getInstance().getActiveAutoBidsForAuction(auctionId);

      if (activeBots.isEmpty()) return; // Không có bot nào gác phòng, giữ nguyên luồng đấu thầu bằng tay

      org.example.server.daos.AutoBidDAO.AutoBidConfig bot1 = activeBots.get(0); // Bot có giá trần cao nhất

      // =========================================================================
      // KỊCH BẢN A: CÓ TỪ 2 BOT TRỞ LÊN ĐỤNG ĐỘ SÁT PHẠT NHAU
      // =========================================================================
      if (activeBots.size() >= 2) {
        org.example.server.daos.AutoBidDAO.AutoBidConfig bot2 = activeBots.get(1); // Bot có giá trần cao thứ nhì

        // Trường hợp biên (Edge Case): Hai tài khoản cài giá trần giống hệt nhau
        if (bot1.getMaxBid().compareTo(bot2.getMaxBid()) == 0) {
          // Nếu giá phòng hiện tại chưa chạm tới mức trần này
          if (currentPrice.compareTo(bot1.getMaxBid()) < 0) {
            java.math.BigDecimal finalPrice = bot1.getMaxBid();
            // Thằng nào đặt cấu hình Bot trước (createdAt nhỏ hơn) sẽ dành chiến thắng dẫn đầu!
            int winnerId = bot1.getCreatedAt().isBefore(bot2.getCreatedAt()) ? bot1.getUserId() : bot2.getUserId();

            executeAutoBidTransaction(auctionId, finalPrice, winnerId, auction);
          }
        }
        // Trường hợp thông thường: Trần Bot 1 lớn hơn hẳn trần Bot 2
        else if (bot1.getMaxBid().compareTo(bot2.getMaxBid()) > 0) {
          // Công thức thép: Giá nhảy vọt lên bằng Trần thằng thua + 1 Bước giá thầu
          java.math.BigDecimal finalPrice = bot2.getMaxBid().add(increment);

          // Chốt chặn an toàn: Nếu tính ra vượt quá trần Bot 1, ép lùi về kịch trần Bot 1
          if (finalPrice.compareTo(bot1.getMaxBid()) > 0) {
            finalPrice = bot1.getMaxBid();
          }

          // Chỉ cập nhật nếu con số tính toán toán học này thực sự cao hơn giá đang hiển thị và Bot 1 chưa dẫn đầu
          if (finalPrice.compareTo(currentPrice) > 0 || auction.getBidderId() != bot1.getUserId()) {
            executeAutoBidTransaction(auctionId, finalPrice, bot1.getUserId(), auction);
          }
        }
      }
      // =========================================================================
      // KỊCH BẢN B: CHỈ CÓ DUY NHẤT 1 BOT CÔ ĐƠN GÁC PHÒNG
      // =========================================================================
      else {
        // Nếu giá trần của Bot đủ lớn để nuốt chửng giá hiện tại và người dẫn đầu phòng không phải chủ Bot
        if (bot1.getMaxBid().compareTo(currentPrice) > 0 && auction.getBidderId() != bot1.getUserId()) {
          java.math.BigDecimal finalPrice = currentPrice.add(increment);
          if (finalPrice.compareTo(bot1.getMaxBid()) > 0) {
            finalPrice = bot1.getMaxBid();
          }
          executeAutoBidTransaction(auctionId, finalPrice, bot1.getUserId(), auction);
        }
      }
    } catch (Exception e) {
      System.err.println("❌ Lỗi xử lý lõi toán học AutoBid: " + e.getMessage());
      e.printStackTrace();
    }
  }

  // Hàm phụ trợ ghi Log lịch sử và phát sóng thông báo giá nhảy vọt do Bot
  private static void executeAutoBidTransaction(int auctionId, java.math.BigDecimal finalPrice, int winnerId, org.example.core.models.entities.Auction auction) throws Exception {
    // 1. Trích xuất tên hiển thị của tài khoản Bot chiến thắng từ hệ thống
    String winnerName = userDAO.getUserByUserId(winnerId).getUserName();

    // 2. ĐÓNG GÓI CHUẨN ĐỒNG BỘ: Khởi tạo thực thể BidTransaction dành cho luồng AutoBid
    BidTransaction autoBidTx = new BidTransaction(finalPrice, java.time.LocalDateTime.now(), winnerId, winnerName);
    autoBidTx.setAuctionId(auctionId);

    // 3. Gọi chung một hàm insert thực thể của DAO giống hệt luồng đặt giá bằng tay
    bidDAO.insertBid(autoBidTx);

    // Cập nhật giá thầu trần mới dẫn đầu vào bảng dữ liệu phiên phòng đấu giá
    auctionDAO.updateHighestPriceByItemId(winnerId, finalPrice);

    // 4. Đóng gói gói tin phát sóng thời gian thực (Bật cờ true báo hiệu giá nhảy do AutoBid)
    org.example.core.dto.bidDTO.BidBroadcastDTO broadcastData = new org.example.core.dto.bidDTO.BidBroadcastDTO(
            auctionId,
            finalPrice.doubleValue(),
            winnerName,
            auction.getEndTime(),
            true // 🔥 ĐÁNH DẤU: Cờ báo hiệu nhảy giá do AutoBid!
    );

    org.example.core.dto.Response broadcastResponse = new org.example.core.dto.Response("NEW_BID", "Hệ thống tự động đẩy giá!", broadcastData);
    org.example.server.network.AuctionServer.broadcastToRoom(auctionId, broadcastResponse);
    System.out.println("🚀 [AUTOBID BROADCAST] Giá phòng " + auctionId + " tự động tăng vọt lên " + finalPrice + " đ bởi Bot của " + winnerName);
  }
}
