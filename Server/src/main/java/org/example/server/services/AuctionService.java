package org.example.server.services;

import org.example.core.dto.auctionDTO.CreateAuctionDTO;
import org.example.core.dto.paymentDTO.PaidHistoryDTO;
import org.example.core.dto.paymentDTO.PendingPaymentsDTO;
import org.example.core.dto.Response;
import org.example.core.exception.DataConflictException;
import org.example.core.exception.InvalidUserDataException;
import org.example.core.exception.ResourceNotFoundException;
import org.example.core.exception.InsufficientBalanceException;
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
 * Dịch vụ quản lý vòng đời phiên đấu giá.
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

  public Auction createAuction(CreateAuctionDTO requestPayLoad) {
    if (requestPayLoad == null) {
      throw new InvalidUserDataException("Dữ liệu yêu cầu tạo phiên đấu giá không được để trống!");
    }

    Item checkItem = requestPayLoad.getItem();
    long durationMinutes = requestPayLoad.getDurationMinutes();
    BigDecimal bidIncrement = requestPayLoad.getBidIncrement();
    LocalDateTime startTime = requestPayLoad.getStartTime();

    validateAuctionPayload(checkItem, durationMinutes, bidIncrement, startTime);

    int auctionId = auctionDAO.createNewAuctionItem(checkItem, durationMinutes, bidIncrement, startTime);
    if (auctionId <= 0) {
      throw new ResourceNotFoundException("Lỗi hệ thống: Không thể khởi tạo phiên đấu giá mới trong cơ sở dữ liệu!");
    }

    Auction newAuction = auctionDAO.getAuctionByAuctionId(auctionId);

    if (newAuction != null) {
      long delayToStart = Duration.between(LocalDateTime.now(), newAuction.getStartTime()).getSeconds();
      if (delayToStart <= 0) {
        scheduler.submit(() -> startAuction(auctionId));
      } else {
        scheduler.schedule(() -> startAuction(auctionId), delayToStart, TimeUnit.SECONDS);
        logger.info("[HẸN GIỜ] Phiên đấu giá mới tạo ID: " + auctionId + " sẽ mở cửa sau " + delayToStart + " giây.");
      }
    }
    return newAuction;
  }

  public void forceCancelAuction(int auctionId, String reason) {
    if (auctionId <= 0) {
      throw new InvalidUserDataException("Mã phiên đấu giá không hợp lệ!");
    }
    Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
    if (auction == null) {
      throw new ResourceNotFoundException("Không tìm thấy phiên đấu giá cần hủy có mã: " + auctionId);
    }
    if (auction.getStatus() == AuctionStatus.CANCELED) {
      throw new DataConflictException("Phiên đấu giá này đã bị hủy bỏ từ trước đó!");
    }
    if (auction.getStatus() == AuctionStatus.PAID) {
      throw new DataConflictException("Không thể hủy phiên đấu giá vì giao dịch đã được thanh toán hoàn tất!");
    }
    auctionDAO.setAuctionStatus(auctionId, AuctionStatus.CANCELED);
    logger.warning("[FORCE CANCEL] Admin đã hủy khẩn cấp phiên " + auctionId + ". Lý do: " + reason);
  }

  public boolean checkoutAuction(int auctionId, int winnerId) {
    if (auctionId <= 0 || winnerId <= 0) {
      throw new InvalidUserDataException("Thông số mã phiên hoặc định danh người chiến thắng không hợp lệ!");
    }

    synchronized (lock) {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction == null) {
        throw new ResourceNotFoundException("Phiên đấu giá không tồn tại trên hệ thống!");
      }
      if (auction.getStatus() == AuctionStatus.PAID) {
        throw new DataConflictException("Phiên đấu giá này đã được thực hiện thanh toán hoàn tất trước đó!");
      }
      if (auction.getStatus() != AuctionStatus.FINISHED) {
        throw new DataConflictException("Phiên đấu giá chưa kết thúc, không thể tiến hành thủ tục thanh toán!");
      }
      if (auction.getBidderId() != winnerId) {
        throw new InvalidUserDataException("Xác thực thất bại: Bạn không phải là người chiến thắng hợp pháp!");
      }

      int sellerId = itemDAO.getOwnerIdByItemId(auction.getItemId());
      User winner = userDAO.getUserByUserId(winnerId);
      User seller = userDAO.getUserByUserId(sellerId);

      BigDecimal bidPrice = auction.getHighestBid();
      if (bidPrice.compareTo(walletDAO.getAvailableBalance(winnerId)) > 0) {
        throw new InsufficientBalanceException("Số dư khả dụng trong ví tài khoản không đủ để thực hiện thanh toán!");
      }

      userDAO.updateBalanceInDB(winnerId, winner.getBalance().subtract(bidPrice));
      userDAO.updateBalanceInDB(sellerId, seller.getBalance().add(bidPrice));

      walletDAO.insertWalletTransaction(winnerId, bidPrice, WalletTransactionType.PAY_AUCTION, auctionId);
      walletDAO.insertWalletTransaction(sellerId, bidPrice, WalletTransactionType.SELL_REVENUE, auctionId);

      auctionDAO.setAuctionStatus(auctionId, AuctionStatus.PAID);
      itemDAO.updateOwnerIdByItemId(auction.getItemId(), winnerId);
      itemDAO.updateItemStatus(auction.getItemId(), ItemStatus.APPROVED);

      logger.info("[CHECKOUT SUCCESS] Phiên " + auctionId + " quyết toán thành công. Winner: " + winnerId);
      return true;
    }
  }

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
      logger.info("Đã tái cấu trúc và phục hồi toàn bộ tiến trình hẹn giờ ngầm hệ thống!");
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi nghiêm trọng khi nạp khôi phục Scheduled Tasks lịch hẹn giờ", e);
    }
  }

  public List<Auction> getAuctionsByStatus(AuctionStatus status) {
    if (status == null) {
      throw new InvalidUserDataException("Trạng thái phiên đấu giá cần tra cứu không được để trống!");
    }
    return auctionDAO.getAllAuctionsByStatus(status);
  }

  public Auction getAuctionByAuctionId(int auctionId) {
    return auctionDAO.getAuctionByAuctionId(auctionId);
  }

  public Auction getAuctionById(int auctionId) {
    if (auctionId <= 0) {
      throw new InvalidUserDataException("Mã phiên đấu giá không hợp lệ!");
    }
    Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
    if (auction == null) {
      throw new ResourceNotFoundException("Không tìm thấy thông tin chi tiết của phiên đấu giá có mã: " + auctionId);
    }
    return auction;
  }

  public List<Integer> getAllItemPaidPending(int userId) {
    if (userId <= 0) {
      throw new InvalidUserDataException("Mã người dùng không hợp lệ để tra cứu danh sách chờ thanh toán!");
    }
    return auctionDAO.getAllAuctionIdFinishedByUserId(userId);
  }

  public List<PendingPaymentsDTO> getAllAuctionsFinished(int userId) {
    if (userId <= 0) {
      throw new InvalidUserDataException("Mã người dùng không hợp lệ để lấy danh sách hóa đơn chờ thanh toán!");
    }
    return auctionDAO.getAllAuctionsFinished(userId);
  }

  public List<PaidHistoryDTO> getAllAuctionsPaid(int userId) {
    if (userId <= 0) {
      throw new InvalidUserDataException("Mã người dùng không hợp lệ để tra cứu lịch sử mua hàng thành công!");
    }
    return auctionDAO.getAllAuctionsPaid(userId);
  }

  public List<Auction> getMarketHistory() {
    return auctionDAO.getAllCompletedAuctions();
  }

  private void startAuction(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction != null && auction.getStatus() == AuctionStatus.OPEN) {
        auctionDAO.setAuctionStatus(auctionId, AuctionStatus.RUNNING);
        logger.info("Phiên " + auctionId + " ĐÃ CHUYỂN SANG TRẠNG THÁI RUNNING!");

        AuctionServer.broadcastToRoom(auctionId, new Response("AUCTION_STARTED", "Phiên đấu giá bắt đầu!"));

        long durationSeconds = Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();
        if (durationSeconds <= 0) durationSeconds = 1;

        scheduler.schedule(() -> endAuction(auctionId), durationSeconds, TimeUnit.SECONDS);
        logger.info("Đã lên lịch ĐÓNG phiên " + auctionId + " sau chính xác " + durationSeconds + " giây.");
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi luồng xử lý tự động kích hoạt startAuction cho phòng: " + auctionId, e);
    }
  }

  private void endAuction(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);

      if (auction != null && auction.getStatus() == AuctionStatus.RUNNING) {
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(auction.getEndTime())) {
          long remainingSeconds = Duration.between(now, auction.getEndTime()).getSeconds();
          if (remainingSeconds <= 0) remainingSeconds = 1;

          scheduler.schedule(() -> endAuction(auctionId), remainingSeconds, TimeUnit.SECONDS);
          logger.info("[ANTI-SNIPING] Tái thiết lập lịch đóng phòng " + auctionId + " muộn thêm " + remainingSeconds + " giây.");
          return;
        }

        auctionDAO.setAuctionStatus(auctionId, AuctionStatus.FINISHED);
        logger.info("Phiên " + auctionId + " ĐÃ KẾT THÚC THÀNH CÔNG (FINISHED)!");

        String winnerName = "Không có ai";
        if (auction.getBidHistory() != null && !auction.getBidHistory().isEmpty()) {
          winnerName = auction.getBidHistory().get(0).getBidderName();
        }

        AuctionServer.broadcastToRoom(auctionId, new Response("AUCTION_ENDED", winnerName));
        scheduler.schedule(() -> cancelIfNotPaid(auctionId), 24, TimeUnit.HOURS);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi luồng kết thúc phòng endAuction cho phòng: " + auctionId, e);
    }
  }

  private void cancelIfNotPaid(int auctionId) {
    try {
      Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
      if (auction != null && auction.getStatus() == AuctionStatus.FINISHED) {
        auctionDAO.setAuctionStatus(auctionId, AuctionStatus.CANCELED);
        logger.warning("🗑Phiên " + auctionId + " tự động chuyển trạng thái CANCELED do quá hạn thanh toán 24h.");
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi thực thi kiểm tra thanh toán tự động quá hạn phòng: " + auctionId, e);
    }
  }

  private void validateAuctionPayload(Item item, long duration, BigDecimal increment, LocalDateTime start) {
    if (item == null) throw new InvalidUserDataException("Vật phẩm đấu giá không tồn tại hoặc chưa được chọn!");
    if (item.getStatus() == ItemStatus.LISTED) throw new InvalidUserDataException("Vật phẩm này hiện đang trong một phiên đấu giá khác!");
    if (duration <= 0) throw new InvalidUserDataException("Thời gian diễn ra phiên đấu giá phải lớn hơn 0 phút!");
    if (increment == null || increment.compareTo(BigDecimal.ZERO) <= 0) throw new InvalidUserDataException("Bước giá thầu phải lớn hơn 0 VNĐ!");
    if (start == null) throw new InvalidUserDataException("Thời gian bắt đầu phiên đấu giá không được để trống!");
    if (start.isBefore(LocalDateTime.now().minusMinutes(1))) throw new InvalidUserDataException("Thời gian bắt đầu phiên đấu giá không được ở trong quá khứ!");
  }
}