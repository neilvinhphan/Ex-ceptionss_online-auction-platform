package org.example.server.services;

import org.example.core.dto.AuctionRequestDTO;
import org.example.core.dto.CreateAuctionDTO;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.models.items.Item;
import org.example.core.shared.enums.AuctionStatus;
import org.example.core.shared.enums.ItemStatus;
import org.example.server.daos.AuctionDAO;
import org.example.server.daos.ItemDAO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionService {

  private static final AuctionDAO auctionDAO = AuctionDAO.getInstance();
  private static final ItemDAO itemDAO = ItemDAO.getInstance();

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

    // Khởi tạo Auction mới (Nó sẽ tự nhận trạng thái WAREHOUSE từ Constructor)
    Auction newAuction = new Auction(checkItem, durationMinutes, bidIncrement);

    // TODO: Gọi AuctionDAO.insert(newAuction) để lưu nháp xuống DB.

    // THÊM DÒNG NÀY VÀO ĐỂ LƯU XUỐNG DB THẬT SỰ NÀY:
    AuctionDAO.getInstance().createNewAuctionItem(checkItem, durationMinutes, bidIncrement);

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
    // Gọi DAO cập nhật ID của người đang trả giá cao nhất vào bảng Auction.

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
                  auctionDAO.setAuctionStatus(a.getAuctionId(), AuctionStatus.FINISHED);
                  System.out.println("Đã tự động đóng phiên: " + a.getAuctionId());
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
}
