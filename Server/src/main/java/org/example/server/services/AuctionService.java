package org.example.server.services;

import org.example.core.dto.auctionDTO.CreateAuctionDTO;
import org.example.core.dto.paymentDTO.PaidHistoryDTO;
import org.example.core.dto.paymentDTO.PendingPaymentsDTO;
import org.example.core.dto.Response;
import org.example.core.models.entities.Auction;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dịch vụ quản lý vòng đời phiên đấu giá (Khởi tạo, Lên lịch mở/đóng phòng,
 * Gia hạn Anti-Sniping và xử lý luồng thanh toán giao dịch chuyển nhượng tài sản).
 */
public class AuctionService {
  private static final Logger logger = Logger.getLogger(AuctionService.class.getName());
  private static volatile AuctionService instance;

  private final Object lock = new Object();
  private final AuctionDAO auctionDAO;
  private final UserDAO userDAO;
  private final ItemDAO itemDAO;
  private final WalletDAO walletDAO;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

  AuctionService(AuctionDAO auctionDAO, UserDAO userDAO, ItemDAO itemDAO, WalletDAO walletDAO) {
    this.walletDAO = walletDAO;
    this.auctionDAO = auctionDAO;
    this.userDAO = userDAO;
    this.itemDAO = itemDAO;
  }

  /**
   * Lấy instance duy nhất (Singleton) của AuctionService (Thread-safe).
   */
  public static AuctionService getInstance() {
    if (instance == null) {
      synchronized (AuctionService.class) {
        if (instance == null) {
          instance = new AuctionService(
                  AuctionDAO.getInstance(),
                  UserDAO.getInstance(),
                  ItemDAO.getInstance(),
                  WalletDAO.getInstance()
          );
        }
      }
    }
    return instance;
  }

  // --- NHÓM PHƯƠNG THỨC GHI DỮ LIỆU & ĐIỀU KHIỂN LUỒNG (WRITE LOGIC) ---

  /**
   * Tiếp nhận thông số payload, thẩm định logic điều kiện và tạo mới phiên đấu giá,
   * đồng thời lập lịch hẹn giờ kích hoạt phòng tự động.
   */
  public Auction createAuction(CreateAuctionDTO requestPayLoad) throws Exception {
    if (requestPayLoad == null) {
      throw new IllegalArgumentException("Dữ liệu yêu cầu tạo phiên đấu giá không được để trống!");
    }

    Item checkItem = requestPayLoad.getItem();
    long durationMinutes = requestPayLoad.getDurationMinutes();
    BigDecimal bidIncrement = requestPayLoad.getBidIncrement();
    LocalDateTime startTime = requestPayLoad.getStartTime();

    validateAuctionPayload(checkItem, durationMinutes, bidIncrement, startTime);

    int auctionId = auctionDAO.createNewAuctionItem(checkItem, durationMinutes, bidIncrement, startTime);
    if (auctionId <= 0) {
      throw new Exception("Lỗi hệ thống: Không thể khởi tạo phiên đấu giá mới trong cơ sở dữ liệu!");
    }

    Auction newAuction = auctionDAO.getAuctionByAuctionId(auctionId);

    // Kích hoạt tác vụ hẹn giờ mở cửa phòng đấu giá ngầm tuần tự
    if (newAuction != null) {
      long delayToStart = Duration.between(LocalDateTime.now(), newAuction.getStartTime()).getSeconds();
      if (delayToStart <= 0) {
        scheduler.submit(() -> startAuction(auctionId));
      } else {
        scheduler.schedule(() -> startAuction(auctionId), delayToStart, TimeUnit.SECONDS);
        logger.info("⏰ [HẸN GIỜ] Phiên đấu giá mới tạo ID: " + auctionId + " sẽ mở cửa sau " + delayToStart + " giây.");
      }
    }
    return newAuction;
  }

  /**
   * Kích hoạt ép buộc hủy bỏ khẩn cấp một phiên đấu giá từ phân hệ điều hành của Admin.
   */
  public void forceCancelAuction(int auctionId, String reason) throws Exception {
    if (auctionId <= 0) {
      throw new IllegalArgumentException("Mã phiên đấu giá không hợp lệ!");
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
    logger.warning("🚨 [FORCE CANCEL] Admin đã hủy khẩn cấp phiên " + auctionId + ". Lý do: " + reason);
  }

  /**
   * Thực hiện thủ tục thanh toán hóa đơn thắng cuộc (Checkout): Khấu trừ tiền ví người mua,
   * cộng doanh thu cho người bán, kết chuyển nhật ký dòng tiền và cập nhật quyền sở hữu vật phẩm.
   */
  public boolean checkoutAuction(int auctionId, int winnerId) throws Exception {
    if (auctionId <= 0 || winnerId <= 0) {
      throw new IllegalArgumentException("Thông số mã phiên hoặc định danh người chiến thắng để thanh toán không hợp lệ!");
    }

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

      if (winner == null || seller == null) {
        throw new Exception("Không tìm thấy đầy đủ hồ sơ thông tin tài khoản người mua hoặc người bán trên hệ thống!");
      }

      BigDecimal bidPrice = auction.getHighestBid();
      if (bidPrice.compareTo(walletDAO.getAvailableBalance(winnerId)) > 0) {
        throw new Exception("Số dư khả dụng trong ví tài khoản không đủ để thực hiện thanh toán!");
      }

      // Khấu trừ tài chính đồng bộ
      userDAO.updateBalanceInDB(winnerId, winner.getBalance().subtract(bidPrice));
      userDAO.updateBalanceInDB(sellerId, seller.getBalance().add(bidPrice));

      walletDAO.insertWalletTransaction(winnerId, bidPrice, WalletTransactionType.PAY_AUCTION, auctionId);
      walletDAO.insertWalletTransaction(sellerId, bidPrice, WalletTransactionType.SELL_REVENUE, auctionId);

      // Cập nhật trạng thái thực thể
      auctionDAO.setAuctionStatus(auctionId, AuctionStatus.PAID);
      itemDAO.updateOwnerIdByItemId(auction.getItemId(), winnerId);
      itemDAO.updateItemStatus(auction.getItemId(), ItemStatus.APPROVED);

      logger.info("💸 [CHECKOUT SUCCESS] Phiên " + auctionId + " đã quyết toán thành công. Winner: " + winnerId + ", Thu về: " + bidPrice);
      return true;
    }
  }

  /**
   * Quét và khôi phục toàn bộ tiến trình hẹn giờ ngầm chạy trong RAM cho các phiên
   * OPEN, RUNNING, FINISHED khi máy chủ Server khởi động lại.
   */
  public void reloadScheduledTasksOnStartup() {
    try {
      LocalDateTime now = LocalDateTime.now();

      List<Auction> openAuctions = auctionDAO.getAllAuctionsByStatus(AuctionStatus.OPEN);
      for (Auction a : openAuctions) {
        long delay = Duration.between(now, a.getStartTime()).getSeconds();
        if (delay <= 0) startAuction(a.getAuctionId());
        else scheduler.schedule(() -> startAuction(a.getAuctionId()), delay, TimeUnit.SECONDS);
      }

      List<Auction> runningAuctions = auctionDAO.getAllAuctionsByStatus(AuctionStatus.RUNNING);
      for (Auction a : runningAuctions) {
        long delay = Duration.between(now, a.getEndTime()).getSeconds();
        if (delay <= 0) endAuction(a.getAuctionId());
        else scheduler.schedule(() -> endAuction(a.getAuctionId()), delay, TimeUnit.SECONDS);
      }

      List<Auction> finishedAuctions = auctionDAO.getAllAuctionsByStatus(AuctionStatus.FINISHED);
      for (Auction a : finishedAuctions) {
        LocalDateTime deadlineToPay = a.getEndTime().plusHours(24);
        long delay = Duration.between(now, deadlineToPay).getSeconds();
        if (delay <= 0) cancelIfNotPaid(a.getAuctionId());
        else scheduler.schedule(() -> cancelIfNotPaid(a.getAuctionId()), delay, TimeUnit.SECONDS);
      }
      logger.info("✅ Đã tái cấu trúc và phục hồi toàn bộ tiến trình hẹn giờ ngầm hệ thống!");
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi nghiêm trọng khi nạp khôi phục Scheduled Tasks lịch hẹn giờ", e);
    }
  }

  // --- NHÓM PHƯƠNG THỨC TRUY VẤN DỮ LIỆU (READ LOGIC) ---

  public List<Auction> getAuctionsByStatus(AuctionStatus status) throws Exception {
    if (status == null) {
      throw new IllegalArgumentException("Trạng thái phiên đấu giá cần tra cứu không được để trống!");
    }
    return auctionDAO.getAllAuctionsByStatus(status);
  }

  public Auction getAuctionByAuctionId(int auctionId) throws Exception {
    return auctionDAO.getAuctionByAuctionId(auctionId);
  }

  public Auction getAuctionById(int auctionId) throws Exception {
    if (auctionId <= 0) {
      throw new IllegalArgumentException("Mã phiên đấu giá không hợp lệ!");
    }
    Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
    if (auction == null) {
      throw new Exception("Không tìm thấy thông tin chi tiết của phiên đấu giá có mã: " + auctionId);
    }
    return auction;
  }

  public List<Integer> getAllItemPaidPending(int userId) throws Exception {
    if (userId <= 0) {
      throw new IllegalArgumentException("Mã người dùng không hợp lệ để tra cứu danh sách chờ thanh toán!");
    }
    return auctionDAO.getAllAuctionIdFinishedByUserId(userId);
  }

  public List<PendingPaymentsDTO> getAllAuctionsFinished(int userId) throws Exception {
    if (userId <= 0) {
      throw new IllegalArgumentException("Mã người dùng không hợp lệ để lấy danh sách hóa đơn chờ thanh toán!");
    }
    return auctionDAO.getAllAuctionsFinished(userId);
  }

  public List<PaidHistoryDTO> getAllAuctionsPaid(int userId) throws Exception {
    if (userId <= 0) {
      throw new IllegalArgumentException("Mã người dùng không hợp lệ để tra cứu lịch sử mua hàng thành công!");
    }
    return auctionDAO.getAllAuctionsPaid(userId);
  }

  public List<Auction> getMarketHistory() throws Exception {
    return auctionDAO.getAllCompletedAuctions();
  }

  // --- NHÓM PHƯƠNG THỨC HẸN GIỜ ĐIỀU PHỐI NỘI BỘ (PRIVATE SCHEDULED TASKS) ---

  /**
   * Tác vụ chuyển trạng thái phòng sang RUNNING công khai và đặt lịch báo giờ đóng phòng đấu giá.
   */
  private void startAuction(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction != null && auction.getStatus() == AuctionStatus.OPEN) {
        auctionDAO.setAuctionStatus(auctionId, AuctionStatus.RUNNING);
        logger.info("🔥 Phiên " + auctionId + " ĐÃ CHUYỂN SANG TRẠNG THÁI RUNNING!");

        AuctionServer.broadcastToRoom(auctionId, new Response("AUCTION_STARTED", "Phiên đấu giá bắt đầu!"));

        long durationSeconds = Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();
        if (durationSeconds <= 0) durationSeconds = 1;

        scheduler.schedule(() -> endAuction(auctionId), durationSeconds, TimeUnit.SECONDS);
        logger.info("⏰ Đã lên lịch ĐÓNG phiên " + auctionId + " sau chính xác " + durationSeconds + " giây.");
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi luồng xử lý tự động kích hoạt startAuction cho phòng: " + auctionId, e);
    }
  }

  /**
   * Tác vụ đóng phòng đấu giá. Kiểm tra ràng buộc thời gian nới lỏng từ cơ chế chống bắn tỉa (Anti-Sniping)
   * trước khi chuyển trạng thái phòng thành FINISHED.
   */
  private void endAuction(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);

      if (auction != null && auction.getStatus() == AuctionStatus.RUNNING) {
        LocalDateTime now = LocalDateTime.now();

        // CHỐT CHẶN ANTI-SNIPING: Nếu phòng được đẩy giờ muộn hơn thời điểm hiện tại
        if (now.isBefore(auction.getEndTime())) {
          long remainingSeconds = Duration.between(now, auction.getEndTime()).getSeconds();
          if (remainingSeconds <= 0) remainingSeconds = 1;

          scheduler.schedule(() -> endAuction(auctionId), remainingSeconds, TimeUnit.SECONDS);
          logger.info("⏳ [ANTI-SNIPING] Tái thiết lập lịch đóng phòng " + auctionId + " muộn thêm " + remainingSeconds + " giây.");
          return;
        }

        auctionDAO.setAuctionStatus(auctionId, AuctionStatus.FINISHED);
        logger.info("🛑 Phiên " + auctionId + " ĐÃ KẾT THÚC THÀNH CÔNG (FINISHED)!");

        String winnerName = "Không có ai";
        if (auction.getBidHistory() != null && !auction.getBidHistory().isEmpty()) {
          winnerName = auction.getBidHistory().get(0).getBidderName();
        }

        AuctionServer.broadcastToRoom(auctionId, new Response("AUCTION_ENDED", winnerName));

        // Thiết lập chốt chặn tự động hủy phiên sau 24 giờ nếu người thắng không thanh toán tiền hàng
        scheduler.schedule(() -> cancelIfNotPaid(auctionId), 24, TimeUnit.HOURS);
        logger.info("⏰ Đã lập lịch hủy khẩn cấp do quá hạn chi tiền sau 24h cho phòng " + auctionId);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi luồng kết thúc phòng endAuction cho phòng: " + auctionId, e);
    }
  }

  /**
   * Tác vụ tự động hủy bỏ kết quả phiên (CANCELED) nếu quá thời gian tạm giữ hóa đơn 24 giờ mà chưa thanh toán thành công.
   */
  private void cancelIfNotPaid(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction != null && auction.getStatus() == AuctionStatus.FINISHED) {
        auctionDAO.setAuctionStatus(auctionId, AuctionStatus.CANCELED);
        logger.warning("🗑️ Phiên " + auctionId + " tự động chuyển trạng thái CANCELED do quá hạn thanh toán hóa đơn 24h.");
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi thực thi kiểm tra thanh toán tự động quá hạn phòng: " + auctionId, e);
    }
  }

  private void validateAuctionPayload(Item item, long duration, BigDecimal increment, LocalDateTime start) throws Exception {
    if (item == null) throw new Exception("Vật phẩm đấu giá không tồn tại hoặc chưa được chọn!");
    if (item.getStatus() == ItemStatus.LISTED) throw new Exception("Vật phẩm này hiện đang trong một phiên đấu giá khác!");
    if (duration <= 0) throw new Exception("Thời gian diễn ra phiên đấu giá phải lớn hơn 0 phút!");
    if (increment == null || increment.compareTo(BigDecimal.ZERO) <= 0) throw new Exception("Bước giá thầu phải lớn hơn 0 VNĐ!");
    if (start == null) throw new Exception("Thời gian bắt đầu phiên đấu giá không được để trống!");
    if (start.isBefore(LocalDateTime.now().minusMinutes(1))) throw new Exception("Thời gian bắt đầu phiên đấu giá không được ở trong quá khứ!");
  }
}