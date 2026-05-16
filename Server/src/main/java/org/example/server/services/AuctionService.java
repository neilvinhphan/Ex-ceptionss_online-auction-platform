package org.example.server.services;

import org.example.core.dto.auctionDTO.CreateAuctionDTO;
import org.example.core.dto.paymentDTO.PaidHistoryDTO;
import org.example.core.dto.paymentDTO.PendingPaymentsDTO;
import org.example.core.dto.Response;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.models.items.Item;
import org.example.core.models.users.User;
import org.example.core.shared.enums.AuctionStatus;
import org.example.core.shared.enums.ItemStatus;
import org.example.core.shared.enums.WalletTransactionType;
import org.example.server.daos.AuctionDAO;
import org.example.server.daos.ItemDAO;
import org.example.server.daos.UserDAO;
import org.example.server.daos.WalletDAO;

// Thay đổi import này theo đúng package chứa mạng của bro
import org.example.server.network.AuctionServer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionService {

  private static final AuctionDAO auctionDAO = AuctionDAO.getInstance();
  private static final UserDAO userDAO = UserDAO.getInstance();
  private static final ItemDAO itemDAO = ItemDAO.getInstance();
  private static final WalletDAO walletDAO = WalletDAO.getInstance();

  // Lập tổ đội 10 luồng chuyên xử lý báo thức
  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

  // ==========================================
  //  1. NHÓM KHỞI TẠO & KIỂM DUYỆT
  // ==========================================

  public static Auction createAuction(CreateAuctionDTO requestPayLoad) throws Exception {
    Item checkItem = requestPayLoad.getItem();
    long durationMinutes = requestPayLoad.getDurationMinutes();
    BigDecimal bidIncrement = requestPayLoad.getBidIncrement();
    LocalDateTime startTime = requestPayLoad.getStartTime();

    if (checkItem == null) throw new Exception("Vật phẩm không tồn tại!");
    if (checkItem.getStatus() == ItemStatus.LISTED)
      throw new Exception("Vật phẩm đang được đấu giá!");

    int auction_id =
        auctionDAO.createNewAuctionItem(checkItem, durationMinutes, bidIncrement, startTime);

    // THÊM ĐOẠN NÀY: Hẹn giờ mở phòng tự động ngay lập tức
    long delayToStart = Duration.between(LocalDateTime.now(), startTime).toMillis();
    if (delayToStart < 0) delayToStart = 0; // Đề phòng lỗi âm thời gian

    scheduler.schedule(
        () -> {
          try {
            startAuction(auction_id); // Hàm chuyển sang RUNNING mà bro đã viết
          } catch (Exception e) {
            e.printStackTrace();
          }
        },
        delayToStart,
        TimeUnit.MILLISECONDS);

    return auctionDAO.getAuctionByAuctionId(auction_id);
  }

  // ==========================================
  // 2. NHÓM VẬN HÀNH TỰ ĐỘNG (AUTO-TRIGGER)
  // ==========================================

  // TRẠM 2: Mở cửa phòng
  private static void startAuction(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction != null && auction.getStatus() == AuctionStatus.OPEN) {

        auctionDAO.setAuctionStatus(auctionId, AuctionStatus.RUNNING);
        System.out.println("🔥 Phiên " + auctionId + " ĐÃ BẮT ĐẦU!");

        // Broadcast gọi Client mở khóa màn hình
        Response startResponse = new Response("AUCTION_STARTED", "Phiên đấu giá bắt đầu!");
        AuctionServer.broadcastToRoom(auctionId, startResponse);

        // Lên dây cót ĐÓNG PHÒNG
        long durationSeconds = auction.getDurationMinutes() * 60L;
        scheduler.schedule(() -> endAuction(auctionId), durationSeconds, TimeUnit.SECONDS);
      }
    } catch (Exception e) {
      System.err.println("Lỗi luồng startAuction: " + e.getMessage());
    }
  }

  // TRẠM 3: Đóng cửa phòng
  private static void endAuction(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction != null && auction.getStatus() == AuctionStatus.RUNNING) {

        auctionDAO.setAuctionStatus(auctionId, AuctionStatus.FINISHED);
        System.out.println("🛑 Phiên " + auctionId + " ĐÃ KẾT THÚC!");

        // Lấy thông tin người thắng để bắn về (Nếu có)
        String winnerName = "Không có ai";
        if (auction.getBidHistory() != null && !auction.getBidHistory().isEmpty()) {
          winnerName = auction.getBidHistory().get(0).getBidderName(); // Giả sử list xếp giảm dần
        }

        // Broadcast khóa màn hình
        Response endResponse = new Response("AUCTION_ENDED", winnerName);
        AuctionServer.broadcastToRoom(auctionId, endResponse);

        // Lên dây cót hủy phòng nếu không thanh toán (Bác lao công 24h)
        scheduler.schedule(() -> cancelIfNotPaid(auctionId), 24, TimeUnit.HOURS);
        System.out.println("⏰ Đã hẹn giờ kiểm tra thanh toán phiên " + auctionId + " sau 24h.");
      }
    } catch (Exception e) {
      System.err.println("Lỗi luồng endAuction: " + e.getMessage());
    }
  }

  // TRẠM 4: Bác lao công 24h
  private static void cancelIfNotPaid(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      // Nếu sau 24h mà trạng thái VẪN là FINISHED (nghĩa là hàm checkoutAuction chưa được gọi)
      if (auction != null && auction.getStatus() == AuctionStatus.FINISHED) {
        auctionDAO.setAuctionStatus(auctionId, AuctionStatus.CANCELED);
        System.out.println(
            "🗑️ Phiên " + auctionId + " đã bị HỦY do người thắng không thanh toán quá 24h!");
      }
    } catch (Exception e) {
      System.err.println("Lỗi luồng cancelIfNotPaid: " + e.getMessage());
    }
  }

  // HÀM BẢO HIỂM: Phục hồi báo thức khi sập Server
  public static void reloadScheduledTasksOnStartup() {
    try {
      LocalDateTime now = LocalDateTime.now();

      // 1. Khôi phục phòng OPEN
      List<Auction> openAuctions = auctionDAO.getAllAuctionsByStatusForCatalog(AuctionStatus.OPEN);
      for (Auction a : openAuctions) {
        long delay = Duration.between(now, a.getStartTime()).getSeconds();
        if (delay <= 0) startAuction(a.getAuctionId());
        else scheduler.schedule(() -> startAuction(a.getAuctionId()), delay, TimeUnit.SECONDS);
      }

      // 2. Khôi phục phòng RUNNING
      List<Auction> runningAuctions =
          auctionDAO.getAllAuctionsByStatusForCatalog(AuctionStatus.RUNNING);
      for (Auction a : runningAuctions) {
        long delay = Duration.between(now, a.getEndTime()).getSeconds();
        if (delay <= 0) endAuction(a.getAuctionId());
        else scheduler.schedule(() -> endAuction(a.getAuctionId()), delay, TimeUnit.SECONDS);
      }

      // 3. Khôi phục phòng FINISHED (Check mốc 24h)
      List<Auction> finishedAuctions =
          auctionDAO.getAllAuctionsByStatusForCatalog(AuctionStatus.FINISHED);
      for (Auction a : finishedAuctions) {
        LocalDateTime deadlineToPay = a.getEndTime().plusHours(24);
        long delay = Duration.between(now, deadlineToPay).getSeconds();
        if (delay <= 0) cancelIfNotPaid(a.getAuctionId());
        else scheduler.schedule(() -> cancelIfNotPaid(a.getAuctionId()), delay, TimeUnit.SECONDS);
      }
      System.out.println("✅ Đã khôi phục toàn bộ tiến trình hẹn giờ ngầm!");
    } catch (Exception e) {
      System.err.println("Lỗi reload task: " + e.getMessage());
    }
  }

  // ==========================================
  // 3. CÁC NHÓM CÒN LẠI (HỖ TRỢ, THANH TOÁN...)
  // ==========================================

  public static List<Auction> getAuctionsByStatus(AuctionStatus status) throws Exception {
    return auctionDAO.getAllAuctionsByStatus(status);
  }

  public static void forceCancelAuction(int auctionId, String reason) throws Exception {
    Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
    if (auction.getStatus() == AuctionStatus.CANCELED)
      throw new Exception("Phiên đấu giá đã bị hủy!");
    auctionDAO.setAuctionStatus(auctionId, AuctionStatus.CANCELED);
  }

  public static Auction getAuctionById(int auctionId) throws Exception {
    return auctionDAO.getAuctionByAuctionId(auctionId);
  }

  public static void updateHighestBid(int auctionId, BidTransaction newBid) throws Exception {
    BigDecimal newPrice = newBid.getAmount();
    int bidderId = newBid.getBidderId();
    boolean isUpdated = auctionDAO.updateHighestPriceByItemId(bidderId, newPrice);
    if (!isUpdated) throw new Exception("Cập nhật giá thất bại!");
  }

  public static boolean checkoutAuction(int auctionId, int winnerId) throws Exception {
    Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
    int sellerId = itemDAO.getOwnerIdByItemId(auction.getItemId());
    User winner = userDAO.getUserByUserId(winnerId);
    User seller = userDAO.getUserByUserId(sellerId);

    if (auction.getStatus() == AuctionStatus.PAID)
      throw new Exception("Phiên đấu giá đã được thanh toán!");
    if (auction.getBidderId() != winnerId) throw new Exception("Bạn không phải người thắng!");
    if (auction.getHighestBid().compareTo(walletDAO.getAvailableBalance(winnerId)) > 0) {
      throw new Exception("Số dư không đủ!");
    }

    BigDecimal bidPrice = auction.getHighestBid();
    userDAO.updateBalanceInDB(winnerId, winner.getBalance().subtract(bidPrice));
    userDAO.updateBalanceInDB(sellerId, seller.getBalance().add(bidPrice));

    walletDAO.insertWalletTransaction(
        winnerId, bidPrice, WalletTransactionType.PAY_AUCTION, auctionId);
    walletDAO.insertWalletTransaction(
        sellerId, bidPrice, WalletTransactionType.SELL_REVENUE, auctionId);

    // Chuyển trạng thái sang PAID, chặn đứng cái "Báo thức 24h"
    auctionDAO.setAuctionStatus(auctionId, AuctionStatus.PAID);
    itemDAO.updateOwnerIdByItemId(auction.getItemId(), winnerId);
    return true;
  }

  public static List<PendingPaymentsDTO> getAllAuctionsFinished(int userId) throws Exception {
    return auctionDAO.getAllAuctionsFinished(userId);
  }

  public static List<PaidHistoryDTO> getAllAuctionsPaid(int userId) throws Exception {
    return auctionDAO.getAllAuctionsPaid(userId);
  }
}
