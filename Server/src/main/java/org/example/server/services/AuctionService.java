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
import org.example.server.network.AuctionServer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionService {

  private final Object lock = new Object();
  private final AuctionDAO auctionDAO;
  private final UserDAO userDAO;
  private final ItemDAO itemDAO;
  private final WalletDAO walletDAO;
  private static volatile AuctionService instance;

  AuctionService(AuctionDAO auctionDAO, UserDAO userDAO, ItemDAO itemDAO, WalletDAO walletDAO) {
    this.walletDAO =  walletDAO;
    this.auctionDAO = auctionDAO;
    this.userDAO = userDAO;
    this.itemDAO = itemDAO;
  }

  public static AuctionService getInstance() {
    if(instance == null) {
      synchronized (AuctionService.class) {
        if(instance == null) {
          instance = new AuctionService(
                  AuctionDAO.getInstance(),
                  UserDAO.getInstance(),
                  ItemDAO.getInstance(),
                  WalletDAO.getInstance()
          );
        }
      }
    } return instance;
  }

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

  public Auction createAuction(CreateAuctionDTO requestPayLoad) throws Exception {
    if (requestPayLoad == null) {
      throw new Exception("Dữ liệu yêu cầu tạo phiên đấu giá không được để trống!");
    }

    Item checkItem = requestPayLoad.getItem();
    long durationMinutes = requestPayLoad.getDurationMinutes();
    BigDecimal bidIncrement = requestPayLoad.getBidIncrement();
    LocalDateTime startTime = requestPayLoad.getStartTime();

    if (checkItem == null) {
      throw new Exception("Vật phẩm đấu giá không tồn tại hoặc chưa được chọn!");
    }
    if (checkItem.getStatus() == ItemStatus.LISTED) {
      throw new Exception("Vật phẩm này hiện đang trong một phiên đấu giá khác!");
    }
    if (durationMinutes <= 0) {
      throw new Exception("Thời gian diễn ra phiên đấu giá phải lớn hơn 0 phút!");
    }
    if (bidIncrement == null || bidIncrement.compareTo(BigDecimal.ZERO) <= 0) {
      throw new Exception("Bước giá thầu phải lớn hơn 0 VNĐ!");
    }
    if (startTime == null) {
      throw new Exception("Thời gian bắt đầu phiên đấu giá không được để trống!");
    }
    if (startTime.isBefore(LocalDateTime.now().minusMinutes(1))) {
      throw new Exception("Thời gian bắt đầu phiên đấu giá không được ở trong quá khứ!");
    }

    int auction_id =
            auctionDAO.createNewAuctionItem(checkItem, durationMinutes, bidIncrement, startTime);

    if (auction_id <= 0) {
      throw new Exception("Lỗi hệ thống: Không thể khởi tạo phiên đấu giá mới trong cơ sở dữ liệu!");
    }

    Auction newAuction = auctionDAO.getAuctionByAuctionId(auction_id);

    // ⏰ KÍCH HOẠT TỰ ĐỘNG: Lên lịch hẹn giờ mở cửa phòng ngay khi User vừa tạo phòng thành công
    if (newAuction != null) {
      long delayToStart = Duration.between(LocalDateTime.now(), newAuction.getStartTime()).getSeconds();
      if (delayToStart <= 0) {
        scheduler.submit(() -> startAuction(auction_id));
      } else {
        scheduler.schedule(() -> startAuction(auction_id), delayToStart, TimeUnit.SECONDS);
        System.out.println("⏰ [HẸN GIỜ] Phiên đấu giá mới tạo ID: " + auction_id + " sẽ mở cửa sau " + delayToStart + " giây.");
      }
    }
    return newAuction;
  }

  private void startAuction(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction != null && auction.getStatus() == AuctionStatus.OPEN) {

        auctionDAO.setAuctionStatus(auctionId, AuctionStatus.RUNNING);
        System.out.println("🔥 Phiên " + auctionId + " ĐÃ CHUYỂN SANG RUNNING!");

        // Broadcast gọi Client mở khóa màn hình vũ khí
        Response startResponse = new Response("AUCTION_STARTED", "Phiên đấu giá bắt đầu!");
        AuctionServer.broadcastToRoom(auctionId, startResponse);

        // 🛡️ VÁ LỖI CHÍ MẠNG: Tính số giây tồn tại của phòng dựa trên khoảng cách thực tế đến endTime trong DB
        long durationSeconds = Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();
        if (durationSeconds <= 0) {
          durationSeconds = 1; // Fallback an toàn bảo vệ hệ thống
        }

        scheduler.schedule(() -> endAuction(auctionId), durationSeconds, TimeUnit.SECONDS);
        System.out.println("⏰ Đã lên lịch ĐÓNG phiên " + auctionId + " sau chính xác " + durationSeconds + " giây.");
      }
    } catch (Exception e) {
      System.err.println("Lỗi luồng startAuction: " + e.getMessage());
    }
  }

  private void endAuction(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction != null && auction.getStatus() == AuctionStatus.RUNNING) {

        auctionDAO.setAuctionStatus(auctionId, AuctionStatus.FINISHED);
        System.out.println("🛑 Phiên " + auctionId + " ĐÃ KẾT THÚC THÀNH CÔNG!");

        String winnerName = "Không có ai";
        if (auction.getBidHistory() != null && !auction.getBidHistory().isEmpty()) {
          winnerName = auction.getBidHistory().get(0).getBidderName();
        }

        Response endResponse = new Response("AUCTION_ENDED", winnerName);
        AuctionServer.broadcastToRoom(auctionId, endResponse);

        scheduler.schedule(() -> cancelIfNotPaid(auctionId), 24, TimeUnit.HOURS);
        System.out.println("⏰ Đã hẹn giờ kiểm tra thanh toán phiên " + auctionId + " sau 24h.");
      }
    } catch (Exception e) {
      System.err.println("Lỗi luồng endAuction: " + e.getMessage());
    }
  }

  private void cancelIfNotPaid(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction != null && auction.getStatus() == AuctionStatus.FINISHED) {
        auctionDAO.setAuctionStatus(auctionId, AuctionStatus.CANCELED);
        System.out.println("🗑️ Phiên " + auctionId + " đã bị HỦY do quá hạn thanh toán 24h.");
      }
    } catch (Exception e) {
      System.err.println("Lỗi luồng cancelIfNotPaid: " + e.getMessage());
    }
  }

  public void reloadScheduledTasksOnStartup() {
    try {
      LocalDateTime now = LocalDateTime.now();

      List<Auction> openAuctions = auctionDAO.getAllAuctionsByStatusForCatalog(AuctionStatus.OPEN);
      for (Auction a : openAuctions) {
        long delay = Duration.between(now, a.getStartTime()).getSeconds();
        if (delay <= 0) startAuction(a.getAuctionId());
        else scheduler.schedule(() -> startAuction(a.getAuctionId()), delay, TimeUnit.SECONDS);
      }

      List<Auction> runningAuctions = auctionDAO.getAllAuctionsByStatusForCatalog(AuctionStatus.RUNNING);
      for (Auction a : runningAuctions) {
        long delay = Duration.between(now, a.getEndTime()).getSeconds();
        if (delay <= 0) endAuction(a.getAuctionId());
        else scheduler.schedule(() -> endAuction(a.getAuctionId()), delay, TimeUnit.SECONDS);
      }

      List<Auction> finishedAuctions = auctionDAO.getAllAuctionsByStatusForCatalog(AuctionStatus.FINISHED);
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

  public List<Auction> getAuctionsByStatus(AuctionStatus status) throws Exception {
    if (status == null) {
      throw new Exception("Trạng thái phiên đấu giá cần tra cứu không được để trống!");
    }
    return auctionDAO.getAllAuctionsByStatusForCatalog(status);
  }

  public List<Integer> getAllItemPaidPending(int userId) throws Exception {
    if (userId <= 0) {
      throw new Exception("Mã người dùng không hợp lệ để tra cứu danh sách chờ thanh toán!");
    }
    return auctionDAO.getAllAuctionIdFinishedByUserId(userId);
  }

  public void forceCancelAuction(int auctionId, String reason) throws Exception {
    if (auctionId <= 0) {
      throw new Exception("Mã phiên đấu giá không hợp lệ!");
    }
    Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
    if (auction == null) {
      throw new Exception("Không tìm thấy phiên đấu giá cần hủy có mã: " + auctionId);
    }
    if (auction.getStatus() == AuctionStatus.CANCELED) {
      throw new Exception("Phiên đấu giá này đã bị hủy bỏ từ trước đó!");
    }
    if (auction.getStatus() == AuctionStatus.PAID) {
      throw new Exception("Không thể hủy phiên đấu giá vì giao dịch đã được thanh toán hoàn tất!");
    }
    auctionDAO.setAuctionStatus(auctionId, AuctionStatus.CANCELED);
  }

  public Auction getAuctionById(int auctionId) throws Exception {
    if (auctionId <= 0) {
      throw new Exception("Mã phiên đấu giá không hợp lệ!");
    }
    Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
    if (auction == null) {
      throw new Exception("Không tìm thấy thông tin chi tiết của phiên đấu giá có mã: " + auctionId);
    }
    return auction;
  }

  public boolean checkoutAuction(int auctionId, int winnerId) throws Exception {
    if (auctionId <= 0) {
      throw new Exception("Mã phiên đấu giá thực hiện thanh toán không hợp lệ!");
    }
    if (winnerId <= 0) {
      throw new Exception("Mã người mua/người thắng cuộc không hợp lệ!");
    }

    // Sử dụng khối synchronized để đảm bảo tại một thời điểm chỉ có 1 luồng được xử lý thanh toán giao dịch này
    synchronized (lock) {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction == null) {
        throw new Exception("Phiên đấu giá không tồn tại trên hệ thống!");
      }

      if (auction.getStatus() == AuctionStatus.PAID) {
        throw new Exception("Phiên đấu giá này đã được thực hiện thanh toán hoàn tất trước đó!");
      }

      if (auction.getStatus() != AuctionStatus.FINISHED) {
        throw new Exception("Phiên đấu giá chưa kết thúc, không thể tiến hành thủ tục thanh toán!");
      }

      if (auction.getBidderId() != winnerId) {
        throw new Exception("Xác thực thất bại: Bạn không phải là người chiến thắng hợp pháp của phiên đấu giá này!");
      }

      int sellerId = itemDAO.getOwnerIdByItemId(auction.getItemId());
      User winner = userDAO.getUserByUserId(winnerId);
      User seller = userDAO.getUserByUserId(sellerId);

      if (winner == null) {
        throw new Exception("Không tìm thấy tài khoản thông tin của người mua trên hệ thống!");
      }
      if (seller == null) {
        throw new Exception("Không tìm thấy tài khoản thông tin của người bán (chủ vật phẩm) trên hệ thống!");
      }

      if (auction.getHighestBid().compareTo(walletDAO.getAvailableBalance(winnerId)) > 0) {
        throw new Exception("Số dư khả dụng trong ví tài khoản không đủ để thực hiện thanh toán!");
      }

      BigDecimal bidPrice = auction.getHighestBid();

      // Thực hiện trừ/cộng tiền và ghi log giao dịch
      userDAO.updateBalanceInDB(winnerId, winner.getBalance().subtract(bidPrice));
      userDAO.updateBalanceInDB(sellerId, seller.getBalance().add(bidPrice));

      walletDAO.insertWalletTransaction(winnerId, bidPrice, WalletTransactionType.PAY_AUCTION, auctionId);
      walletDAO.insertWalletTransaction(sellerId, bidPrice, WalletTransactionType.SELL_REVENUE, auctionId);

      // Chuyển trạng thái và đổi chủ sở hữu vật phẩm
      auctionDAO.setAuctionStatus(auctionId, AuctionStatus.PAID);
      itemDAO.updateOwnerIdByItemId(auction.getItemId(), winnerId);

      return true;
    }
  }

  public List<PendingPaymentsDTO> getAllAuctionsFinished(int userId) throws Exception {
    if (userId <= 0) {
      throw new Exception("Mã người dùng không hợp lệ để lấy danh sách hóa đơn chờ thanh toán!");
    }
    return auctionDAO.getAllAuctionsFinished(userId);
  }

  public List<PaidHistoryDTO> getAllAuctionsPaid(int userId) throws Exception {
    if (userId <= 0) {
      throw new Exception("Mã người dùng không hợp lệ để tra cứu lịch sử mua hàng thành công!");
    }
    return auctionDAO.getAllAuctionsPaid(userId);
  }
}