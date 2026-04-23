package org.example.server.services;

import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.models.items.Item;
import org.example.core.shared.enums.AuctionStatus;
import org.example.core.shared.enums.ItemStatus;
import org.example.server.daos.AuctionDAO;
import org.example.server.daos.ItemDAO;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {

  private static final AuctionDAO auctionDAO = AuctionDAO.getInstance();
  private static final ItemDAO itemDAO = ItemDAO.getInstance();

  // ==========================================
  // 📦 1. NHÓM KHỞI TẠO (CHUẨN BỊ LÊN SÀN)
  // ==========================================

  public static Auction createAuction(int itemId, long durationMinutes) throws Exception {
    // TODO 1: Gọi ItemDAO lấy Item lên, check xem có tồn tại không.
    // TODO 2: Check xem Item này có đang bị khóa ở phiên đấu giá khác không.
    Item checkItem = ItemDAO.getInstance().getItemById(itemId);
    if (itemDAO == null) {
      throw new Exception("Item không tồn tại!");
    }

    //    ItemStatus checkStatus = ItemDAO.getInstance().getItemStatusById(itemId);

    // Khởi tạo Auction mới (Nó sẽ tự nhận trạng thái WAREHOUSE từ Constructor của bro)
    // Auction newAuction = new Auction(item, durationMinutes);

    // TODO 3: Gọi AuctionDAO.insert(newAuction) để lưu nháp xuống DB.

    return null; // Trả về newAuction sau khi hoàn thiện TODO
  }

  public static List<Auction> getAuctionsByStatus(AuctionStatus status) throws Exception {
    // TODO: Gọi DAO lấy danh sách các phòng đấu giá theo trạng thái (Ví dụ: Lấy các phòng RUNNING
    //    List<Auction> auction = AuctionDAO.getInstance().getAllAuctionByStatus(status);
    // để show lên UI)
    return null;
  }

  // ==========================================
  // 🚀 2. NHÓM VẬN HÀNH (ĐIỀU KHIỂN LUỒNG)
  // ==========================================

  public static void openAuction(int auctionId) throws Exception {
    // 1. Lôi phòng đấu giá từ DB lên
    // Auction auction = auctionDAO.getAuctionById(auctionId);
    // if (auction == null) throw new Exception("Không tìm thấy phiên đấu giá!");

    // 2. Ra lệnh cho Entity tự chạy logic của nó
    // auction.start(LocalDateTime.now());

    // 3. Lưu trạng thái mới (RUNNING) và startTime, endTime xuống Database
    // auctionDAO.updateAuction(auction);
  }

  public static void forceCancelAuction(int auctionId, String reason) throws Exception {
    // TODO: Lấy Auction lên, set trạng thái thành CANCELED và update xuống DB.
    // (Dành cho Admin hoặc người bán hủy ngang khi có biến)
  }

  // ==========================================
  // 🔄 3. NHÓM HỖ TRỢ ĐẤU GIÁ (CHO BIDDING SERVICE GỌI)
  // ==========================================

  public static Auction getAuctionById(int auctionId) throws Exception {
    // TODO: Lấy thông tin phòng đấu giá. BiddingService sẽ dùng hàm này để check liên tục.
    return null;
  }

  public static void updateHighestBid(int auctionId, BidTransaction newBid) throws Exception {
    // TODO: Gọi DAO cập nhật ID của người đang trả giá cao nhất vào bảng Auction.
  }

  // ==========================================
  // ⏱️ 4. NHÓM TỰ ĐỘNG ĐÓNG PHÒNG (AUTO-CLOSE)
  // ==========================================

  // Dùng Lazy Check hay Background Job (Luồng ngầm) để xử lý các phòng hết giờ?

}
