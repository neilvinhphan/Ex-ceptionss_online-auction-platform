package org.example.server.services;

import org.example.core.dto.CreateAuctionDTO;
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

import java.math.BigDecimal;
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

  // ==========================================
  //  1. NHÓM KHỞI TẠO (CHUẨN BỊ LÊN SÀN)
  // ==========================================

  public static Auction createAuction(CreateAuctionDTO requestPayLoad) throws Exception {

    Item checkItem = requestPayLoad.getItem();
    long durationMinutes = requestPayLoad.getDurationMinutes();
    BigDecimal bidIncrement = requestPayLoad.getBidIncrement();

    // Check sự tồn tại của vật phẩm
    if (checkItem == null) {
      throw new Exception("Vật phẩm không tồn tại!");
    }

    // Check trạng thái của vật phẩm (có đang được đấu giá không)
    ItemStatus checkStatus = checkItem.getStatus();
    if (checkStatus == ItemStatus.LISTED) {
      throw new Exception("Vật phẩm đang được đấu giá!");
    }

    // TODO: Gọi AuctionDAO.insert(newAuction) để lưu nháp xuống DB.

    // THÊM DÒNG NÀY VÀO ĐỂ LƯU XUỐNG DB THẬT SỰ NÀY:
    int auction_id =
        AuctionDAO.getInstance().createNewAuctionItem(checkItem, durationMinutes, bidIncrement);

    Auction newAuction = auctionDAO.getAuctionByAuctionId(auction_id);

    return newAuction;
  }

  // Lấy các phiên đấu giá theo trạng thái
  public static List<Auction> getAuctionsByStatus(AuctionStatus status) throws Exception {
    // Gọi DAO lấy danh sách các phòng đấu giá theo trạng thái (Ví dụ: Lấy các phòng RUNNING)
    List<Auction> auction = AuctionDAO.getInstance().getAllAuctionsByStatus(status);
    return auction;
  }

  // ==========================================
  // 2. NHÓM VẬN HÀNH (ĐIỀU KHIỂN LUỒNG)
  // ==========================================

  public static void forceCancelAuction(int auctionId, String reason) throws Exception {
    // Lấy Auction lên, set trạng thái thành CANCELED và update xuống DB.
    // (Dành cho Admin hoặc người bán hủy ngang khi có biến)
    Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
    AuctionStatus status = auction.getStatus();
    if (status == AuctionStatus.CANCELED) throw new Exception("Phiên ấu giá đã bị hủy!");

    auctionDAO.setAuctionStatus(auctionId, AuctionStatus.CANCELED);
  }

  // ==========================================
  // 3. NHÓM HỖ TRỢ ĐẤU GIÁ (CHO BIDDING SERVICE GỌI)
  // ==========================================

  public static Auction getAuctionById(int auctionId) throws Exception {
    // Lấy thông tin phòng đấu giá. BiddingService sẽ dùng hàm này để check liên tục.
    Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
    return auction;
  }

  public static void updateHighestBid(int auctionId, BidTransaction newBid) throws Exception {
    // Móc tiền và ID người Bid ra
    BigDecimal newPrice = newBid.getAmount();
    int bidderId = newBid.getBidderId();

    // Gọi DAO cập nhật ID của người đang trả giá cao nhất và Giá hiện tại vào bảng Auction.
    boolean isUpdated = auctionDAO.updateHighestPriceByItemId(bidderId, newPrice);

    if (!isUpdated) {
      throw new Exception("Cập nhật giá thất bại, không tìm thấy phiên đấu giá!");
    }
  }

  // ==========================================
  // 4. NHÓM TỰ ĐỘNG ĐÓNG PHÒNG (AUTO-CLOSE)
  // ==========================================

  // Dùng Background Job (Luồng ngầm) để xử lý các phòng hết giờ
  // Khai báo Scheduler (Quản lý luồng dọn dẹp)
  private static ScheduledExecutorService scheduler;

  // Khởi động luồng ngầm
  public static void startAutoCloseJob() {
    // Nếu đã chạy rồi thì không khởi tạo lại
    if (scheduler != null && !scheduler.isShutdown()) {
      return;
    }

    scheduler = Executors.newScheduledThreadPool(1);

    Runnable autoCloseTask =
        new Runnable() {
          @Override
          public void run() {
            try {
              System.out.println(
                  "[Background Job] Quét phiên đấu giá hết hạn lúc: " + LocalDateTime.now());

              // Lấy list các phiên RUNNING đã qua giờ endTime
              List<Auction> expiredAuctions =
                  auctionDAO.getAllAuctionsByStatus(AuctionStatus.RUNNING);

              // Lặp qua list và đổi trạng thái thành FINISHED
              for (Auction a : expiredAuctions) {
                if (LocalDateTime.now().isAfter(a.getEndTime())) {
                  if (a.getId() > 0) {
                    auctionDAO.setAuctionStatus(a.getAuctionId(), AuctionStatus.FINISHED);
                    System.out.println("Đã tự động đóng phiên: " + a.getAuctionId());
                  }
                }
              }

            } catch (Exception e) {
              System.err.println("Lỗi luồng Auto Close: " + e.getMessage());
            }
          }
        };

    Runnable autoCancelTask =
        new Runnable() {
          @Override
          public void run() {
            try {
              System.out.println(
                  "[Background Job] Quét trạng thái thanh toán: " + LocalDateTime.now());

              // Lấy list các phiên FINISHED
              List<Auction> finishedAuctions =
                  auctionDAO.getAllAuctionsByStatus(AuctionStatus.FINISHED);

              // Lặp qua list và đổi trạng thái thành CANCELED
              for (Auction a : finishedAuctions) {
                LocalDateTime deadlineToPay = a.getEndTime().plusHours(24);

                if (LocalDateTime.now().isAfter(deadlineToPay)) {
                  if (a.getId() > 0) {
                    auctionDAO.setAuctionStatus(a.getAuctionId(), AuctionStatus.CANCELED);
                    System.out.println(
                        "Phiên: "
                            + a.getAuctionId()
                            + " đã bị hủy do người thắng không thanh toán!");
                  }
                }
              }

            } catch (Exception e) {
              System.err.println("Lỗi luồng Auto Close: " + e.getMessage());
            }
          }
        };

    // Đặt lịch chạy: Bắt đầu sau (initialDelay) PHÚT, lặp lại mỗi (period) PHÚT
    scheduler.scheduleAtFixedRate(autoCloseTask, 0, 1, TimeUnit.MINUTES);
    System.out.println("Đã kích hoạt hệ thống Auto-Close ngầm!");
  }

  // Hàm dọn dẹp khi sập server
  public static void stopAutoCloseJob() {
    if (scheduler != null) {
      scheduler.shutdown();
      System.out.println("Đã tắt hệ thống Auto-Close ngầm!");
    }
  }

  // ==========================================
  // 5. NHÓM XỬ LÝ THANH TOÁN
  // ==========================================

  public static void checkoutAuction(int auctionId, int winnerId) throws Exception {
    Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);

    int sellerId = itemDAO.getOwnerIdByItemId(auction.getItemId());

    User winner = userDAO.getUserByUserId(winnerId);
    User seller = userDAO.getUserByUserId(sellerId);

    if (!auctionDAO.getAuctionStatus(auctionId).equals(AuctionStatus.FINISHED)) {
      throw new Exception("Phiên đấu giá đã được thanh toán!");
    }
    if (auction.getBidderId() != winnerId) {
      throw new Exception("Xảy ra lỗi! Bạn không phải người thắng đấu giá!");
    }

    if (auction.getHighestBid().compareTo(walletDAO.getAvailableBalance(winnerId)) > 0) {
      throw new Exception("Số dư khả dụng không đủ!");
    }
    // Trừ tiền của người mua
    BigDecimal payingAmount = winner.getBalance().subtract(auction.getHighestBid());
    userDAO.updateBalanceInDB(winnerId, payingAmount);

    // Cộng tiền cho người bán
    BigDecimal revenue = seller.getBalance().add(auction.getHighestBid());
    userDAO.updateBalanceInDB(sellerId, revenue);

    // Cập nhật trạng thái phiên
    auctionDAO.setAuctionStatus(auctionId, AuctionStatus.PAID);

    // Cập nhật người sở hữu
    itemDAO.updateOwnerIdByItemId(auction.getItemId(), winnerId);

    // Insert hóa đơn biến động số dư
    // Winner
    walletDAO.insertWalletTransaction(
        winnerId, payingAmount, WalletTransactionType.PAY_AUCTION, auctionId);
    // Seller
    walletDAO.insertWalletTransaction(
        sellerId, revenue, WalletTransactionType.SELL_REVENUE, auctionId);
  }
}
