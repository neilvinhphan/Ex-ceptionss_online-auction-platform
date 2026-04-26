package org.example.server.services;

import org.example.core.dto.AuctionRequestDTO;
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
  //  1. NHÓM KHỞI TẠO (CHUẨN BỊ LÊN SÀN)
  // ==========================================

  public static Auction createAuction(AuctionRequestDTO requestPayLoad) throws Exception {

    Item checkItem = requestPayLoad.getItem();
    long durationMinutes = requestPayLoad.getDurationMinutes();

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
    Auction newAuction = new Auction(checkItem, durationMinutes);

    // TODO 3: Gọi AuctionDAO.insert(newAuction) để lưu nháp xuống DB.

    return newAuction;
  }

  // Lấy các phiên đấu giá theo trạng thái
  public static List<Auction> getAuctionsByStatus(AuctionStatus status) throws Exception {
    // TODO: Gọi DAO lấy danh sách các phòng đấu giá theo trạng thái (Ví dụ: Lấy các phòng RUNNING
    List<Auction> auction = AuctionDAO.getInstance().getAllAuctionsByStatus(status);
    return auction;
  }

  // ==========================================
  // 2. NHÓM VẬN HÀNH (ĐIỀU KHIỂN LUỒNG)
  // ==========================================

  public static void openAuction(int auctionId) throws Exception {
    // Lấy phiên đấu giá từ DB lên
    Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
    if (auction == null) throw new Exception("Không tìm thấy phiên đấu giá!");

    // Bắt đầu phiên đấu giá (Auction.java)
    auction.start(LocalDateTime.now());

    // Lưu trạng thái mới (RUNNING) và startTime, endTime xuống Database
    auctionDAO.setAuctionStatus(auctionId, AuctionStatus.RUNNING);
  }

  public static void forceCancelAuction(int auctionId, String reason) throws Exception {
    // TODO: Lấy Auction lên, set trạng thái thành CANCELED và update xuống DB.
    // (Dành cho Admin hoặc người bán hủy ngang khi có biến)
    Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
    AuctionStatus status = auction.getStatus();
    if (status == AuctionStatus.CANCELED) throw new Exception("Phiên ấu giá đã bị hủy!");

    auctionDAO.setAuctionStatus(auctionId, AuctionStatus.CANCELED);

    // Còn reason chưa biết lưu đâu
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

  // Dùng Lazy Check hay Background Job (Luồng ngầm) để xử lý các phòng hết giờ?

}
