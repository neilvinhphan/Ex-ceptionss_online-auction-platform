package org.example.server.daos;

import org.example.core.dto.paymentDTO.PaidHistoryDTO;
import org.example.core.dto.paymentDTO.PendingPaymentsDTO;
import org.example.core.exception.DatabaseAccessException;
import org.example.core.exception.ResourceNotFoundException;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;
import org.example.core.models.users.User;
import org.example.core.shared.enums.AuctionStatus;
import org.example.server.config.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Lớp truy cập dữ liệu (DAO) quản lý các thực thể và trạng thái phòng đấu giá (Auction). */
public class AuctionDAO {
  private static final Logger logger = Logger.getLogger(AuctionDAO.class.getName());
  private static volatile AuctionDAO instance;
  private static final ItemDAO itemDAO = ItemDAO.getInstance();

  private AuctionDAO() {}

  /** Lấy instance duy nhất (Singleton) của AuctionDAO dưới cơ chế Thread-safe. */
  public static AuctionDAO getInstance() {
    if (instance == null) {
      synchronized (AuctionDAO.class) {
        if (instance == null) {
          instance = new AuctionDAO();
        }
      }
    }
    return instance;
  }

  // --- NHÓM PHƯƠNG THỨC TẠO / CẬP NHẬT (WRITE) ---

  /**
   * Tạo một phiên đấu giá mới từ sản phẩm có sẵn.
   *
   * @param item Đối tượng sản phẩm cần đấu giá.
   * @param timeDurationMinutes Thời lượng diễn ra phiên (tính bằng phút).
   * @param bidIncrement Bước giá tối thiểu cho mỗi lượt đặt.
   * @param startTime Thời gian bắt đầu kích hoạt phòng.
   * @return ID tự tăng của phiên đấu giá vừa tạo, hoặc -1 nếu thất bại.
   */
  public int createNewAuctionItem(
      Item item, long timeDurationMinutes, BigDecimal bidIncrement, LocalDateTime startTime) {
    String sql =
        "INSERT INTO auction (items_id, start_price, bid_increment, end_time, start_time) VALUES (?,?,?,?,?)";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, item.getItemId());
      ps.setBigDecimal(2, item.getStartingPrice());
      ps.setBigDecimal(3, bidIncrement);

      LocalDateTime endTime = startTime.plusMinutes(timeDurationMinutes);
      ps.setTimestamp(4, Timestamp.valueOf(endTime));
      ps.setTimestamp(5, Timestamp.valueOf(startTime));

      if (ps.executeUpdate() > 0) {
        try (ResultSet rs = ps.getGeneratedKeys()) {
          if (rs.next()) {
            return rs.getInt(1);
          }
        }
      }
      return -1;
    } catch (SQLException e) {
      logger.log(
          Level.SEVERE, "Lỗi khi tạo phiên đấu giá mới cho sản phẩm ID: " + item.getItemId(), e);
      throw new DatabaseAccessException("Không thể tạo phiên đấu giá mới", e);
    }
  }

  /** Cập nhật trạng thái hệ thống của một phiên đấu giá. */
  public void setAuctionStatus(int auctionId, AuctionStatus status) {
    String sql = "UPDATE auction SET status = ? WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, status.name());
      ps.setInt(2, auctionId);
      ps.executeUpdate();
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi cập nhật trạng thái cho Auction ID: " + auctionId, e);
      throw new DatabaseAccessException("Cập nhật trạng thái phiên đấu giá thất bại", e);
    }
  }

  /** Gia hạn hoặc thay đổi thời gian kết thúc của một phiên đấu giá. */
  public boolean updateAuctionEndTime(int auctionId, LocalDateTime endTime) {
    String sql = "UPDATE auction SET end_time = ? WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setTimestamp(1, Timestamp.valueOf(endTime));
      ps.setInt(2, auctionId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi cập nhật thời gian kết thúc phòng ID: " + auctionId, e);
      throw new DatabaseAccessException("Cập nhật thời gian kết thúc thất bại", e);
    }
  }

  // --- NHÓM PHƯƠNG THỨC TRUY VẤN ĐƠN LẺ (READ SINGLE) ---

  /** Tìm và nạp chi tiết phòng đấu giá kèm thực thể Item đa hình và thông tin Winner. */
  public Auction getAuctionByAuctionId(int auctionId) {
    String sql = "SELECT * FROM auction WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          Auction auction = new Auction();
          auction.setAuctionId(rs.getInt("auction_id"));
          auction.setItemId(rs.getInt("items_id"));
          auction.setStatus(AuctionStatus.valueOf(rs.getString("status").toUpperCase()));

          Timestamp startTs = rs.getTimestamp("start_time");
          if (startTs != null) auction.setStartTime(startTs.toLocalDateTime());

          Timestamp endTs = rs.getTimestamp("end_time");
          if (endTs != null) auction.setEndTime(endTs.toLocalDateTime());

          auction.setBidIncrement(rs.getBigDecimal("bid_increment"));
          auction.setHighestBid(rs.getBigDecimal("highest_price"));
          auction.setBidderId(rs.getInt("bidder_id"));

          if (auction.getStartTime() != null && auction.getEndTime() != null) {
            long minutes =
                java.time.Duration.between(auction.getStartTime(), auction.getEndTime())
                    .toMinutes();
            auction.setDurationMinutes(minutes);
          }

          try {
            Item item = itemDAO.getItemById(auction.getItemId());
            auction.setItem(item);
          } catch (Exception e) {
            logger.log(
                Level.WARNING, "Lỗi nạp kèm Item trong getAuctionByAuctionId: " + e.getMessage());
          }

          if (auction.getBidderId() > 0) {
            try {
              User winner = UserDAO.getInstance().getUserByUserId(auction.getBidderId());
              if (winner != null) {
                auction.setHighestBidderName(winner.getUserName());
              }
            } catch (Exception e) {
              logger.log(Level.WARNING, "Bỏ qua lỗi lấy tên người thắng: " + e.getMessage());
            }
          }
          return auction;
        }
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi truy vấn thông tin Auction ID: " + auctionId, e);
      throw new DatabaseAccessException("Truy vấn phòng đấu giá thất bại", e);
    }
    throw new ResourceNotFoundException("Không tìm thấy phòng đấu giá với ID: " + auctionId);
  }

  /**
   * Lấy bước giá quy định của một phòng đấu giá cụ thể.
   */
  public BigDecimal getBidIncrementByAuctionId(int auctionId) {
    String sql = "SELECT bid_increment FROM auction WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getBigDecimal("bid_increment");
        }
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi lấy bước giá phòng ID: " + auctionId, e);
      throw new DatabaseAccessException("Truy vấn bước giá thất bại", e);
    }
    throw new ResourceNotFoundException("Không tìm thấy bước giá cho phiên đấu giá ID: " + auctionId);
  }

  // --- NHÓM PHƯƠNG THỨC TRUY VẤN DANH SÁCH (READ LISTS / COLLECTIONS) ---
  /**
   * Lấy danh sách catalog phòng đấu giá, nặn kèm dữ liệu cấu trúc vật phẩm đa hình (Art, Electronics, Vehicle).
   */
  public List<Auction> getAllAuctionsByStatus(AuctionStatus status) {
    List<Auction> auctions = new ArrayList<>();
    String sql =
        "SELECT a.auction_id, a.items_id, a.start_time, a.end_time, a.status, a.bid_increment, a.bidder_id,"
            + " i.items_name AS item_name, i.type AS item_type, i.start_price, i.image, i.description, i.owner_id,"
            + " COALESCE(MAX(b.bid_amount), i.start_price) AS highest_price "
            + " FROM auction a JOIN items i ON a.items_id = i.items_id LEFT JOIN bid b ON a.auction_id = b.auction_id "
            + " WHERE a.status = ? GROUP BY a.auction_id, a.items_id, a.start_time, a.end_time, a.status,"
            + " i.items_name, i.type, i.start_price, i.image, i.description, i.owner_id";

    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, String.valueOf(status));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Auction auction = new Auction();
          auction.setAuctionId(rs.getInt("auction_id"));
          auction.setItemId(rs.getInt("items_id"));
          auction.setBidderId(rs.getInt("bidder_id"));
          auction.setOwnerId(rs.getInt("owner_id"));

          String statusStr = rs.getString("status");
          if (statusStr != null) {
            auction.setStatus(AuctionStatus.valueOf(statusStr.toUpperCase()));
          }
          auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
          auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
          auction.setHighestBid(rs.getBigDecimal("highest_price"));
          auction.setBidIncrement(rs.getBigDecimal("bid_increment"));

          List<BidTransaction> bidTransactions =
              BidDAO.getInstance().getBidHistoryByAuctionId(auction.getAuctionId());
          auction.setBidHistory(bidTransactions);

          String itemType = rs.getString("item_type");
          Item item = null;
          if (itemType != null) {
            switch (itemType.toUpperCase()) {
              case "ART" -> item = new ArtItem();
              case "ELECTRONICS" -> item = new ElectronicsItem();
              case "VEHICLE" -> item = new VehicleItem();
            }
            if (item != null) {
              item.setItemId(rs.getInt("items_id"));
              item.setItemName(rs.getString("item_name"));
              item.setType(itemType);
              item.setStartingPrice(rs.getBigDecimal("start_price"));
              item.setImage(rs.getString("image"));
              item.setDescription(rs.getString("description"));
              auction.setItem(item);
            }
          }
          auctions.add(auction);
        }
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi dựng danh mục Catalog theo trạng thái: " + status, e);
      throw new DatabaseAccessException("Tải dữ liệu Catalog thất bại", e);
    }
    return auctions;
  }

  /**
   * Lấy danh sách các phiên đấu giá đã hoàn thành kết thúc (FINISHED hoặc PAID) làm dữ lịch sử thị
   * trường.
   */
  public List<Auction> getAllCompletedAuctions() {
    List<Auction> auctions = new ArrayList<>();
    String sql =
        "SELECT a.auction_id, a.items_id, a.start_time, a.end_time, a.status, a.bid_increment, a.bidder_id, "
            + "i.items_name AS item_name, i.type AS item_type, i.start_price, i.image, i.description, i.owner_id, "
            + "COALESCE(MAX(b.bid_amount), i.start_price) AS highest_price, COUNT(b.bid_amount) AS total_bids "
            + "FROM auction a JOIN items i ON a.items_id = i.items_id LEFT JOIN bid b ON a.auction_id = b.auction_id "
            + "WHERE a.status IN ('FINISHED', 'PAID') GROUP BY a.auction_id, a.items_id, a.start_time, a.end_time, "
            + "a.status, a.bid_increment, a.bidder_id, i.items_name, i.type, i.start_price, i.image, i.description, i.owner_id "
            + "ORDER BY a.end_time DESC";

    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {

      while (rs.next()) {
        Auction auction = new Auction();
        auction.setAuctionId(rs.getInt("auction_id"));
        auction.setItemId(rs.getInt("items_id"));
        auction.setBidderId(rs.getInt("bidder_id"));
        auction.setOwnerId(rs.getInt("owner_id"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
          auction.setStatus(AuctionStatus.valueOf(statusStr.toUpperCase()));
        }

        if (rs.getTimestamp("start_time") != null) {
          auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        }
        if (rs.getTimestamp("end_time") != null) {
          auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        }

        auction.setHighestBid(rs.getBigDecimal("highest_price"));
        auction.setBidIncrement(rs.getBigDecimal("bid_increment"));
        auction.setTotalBids(rs.getInt("total_bids"));

        String itemType = rs.getString("item_type");
        Item item = null;
        if (itemType != null) {
          switch (itemType.toUpperCase()) {
            case "ART" -> item = new ArtItem();
            case "ELECTRONICS" -> item = new ElectronicsItem();
            case "VEHICLE" -> item = new VehicleItem();
          }
          if (item != null) {
            item.setItemId(rs.getInt("items_id"));
            item.setItemName(rs.getString("item_name"));
            item.setType(itemType);
            item.setStartingPrice(rs.getBigDecimal("start_price"));
            item.setImage(rs.getString("image"));
            item.setDescription(rs.getString("description"));
            auction.setItem(item);
          }
        }
        auctions.add(auction);
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi hệ thống khi trích xuất Market History", e);
      throw new DatabaseAccessException("Không thể lấy lịch sử giao dịch thị trường", e);
    }
    return auctions;
  }

  /** Lấy danh sách các hóa đơn chờ thanh toán (FINISHED) của người dùng thắng cuộc cụ thể. */
  public List<PendingPaymentsDTO> getAllAuctionsFinished(int userId) {
    List<PendingPaymentsDTO> pendingPaymentsDTOs = new ArrayList<>();
    String sql =
        "SELECT auction_id, items_id, highest_price, end_time FROM auction WHERE status = 'FINISHED' AND bidder_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          PendingPaymentsDTO pendingPaymentsDTO = new PendingPaymentsDTO();
          pendingPaymentsDTO.setAuctionId(rs.getInt("auction_id"));
          pendingPaymentsDTO.setItemName(itemDAO.getItemNameByItemId(rs.getInt("items_id")));
          pendingPaymentsDTO.setWinPrice(rs.getBigDecimal("highest_price"));
          pendingPaymentsDTO.setEndDate(
              rs.getTimestamp("end_time").toLocalDateTime().plusHours(24));
          pendingPaymentsDTOs.add(pendingPaymentsDTO);
        }
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi nạp hóa đơn chờ thanh toán của User ID: " + userId, e);
      throw new DatabaseAccessException("Tải danh sách hóa đơn chờ thanh toán thất bại", e);
    }
    return pendingPaymentsDTOs;
  }

  /** Lấy danh sách các phòng đấu giá đã hoàn tất chi tiền (PAID) của người dùng. */
  public List<PaidHistoryDTO> getAllAuctionsPaid(int userId) {
    List<PaidHistoryDTO> paidHistoryDTOs = new ArrayList<>();
    String sql =
        """
        SELECT i.items_name, i.type AS category, a.highest_price AS final_price, w.created_at AS paid_date
        FROM auction a JOIN wallet_transaction w ON a.auction_id = w.reference_id
        JOIN items i ON a.items_id = i.items_id
        WHERE a.status = 'PAID' AND a.bidder_id = ? AND w.transaction_type = 'PAY_AUCTION'""";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          PaidHistoryDTO paidHistoryDTO = new PaidHistoryDTO();
          paidHistoryDTO.setCategory(rs.getString("category"));
          paidHistoryDTO.setPaidDate(rs.getTimestamp("paid_date").toLocalDateTime());
          paidHistoryDTO.setFinalPrice(rs.getBigDecimal("final_price"));
          paidHistoryDTO.setItemName(rs.getString("items_name"));
          paidHistoryDTOs.add(paidHistoryDTO);
        }
      }
      return paidHistoryDTOs;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi lấy lịch sử thanh toán thành công của User ID: " + userId, e);
      throw new DatabaseAccessException("Tải lịch sử giao dịch thanh toán thất bại", e);
    }
  }

  /** Lấy nhanh mảng ID các phòng đấu giá mà người dùng này đã thắng nhưng ở trạng thái FINISHED. */
  public List<Integer> getAllAuctionIdFinishedByUserId(int userId) {
    List<Integer> auctionIds = new ArrayList<>();
    String sql = "SELECT auction_id FROM auction WHERE bidder_id = ? AND status = 'FINISHED'";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          auctionIds.add(rs.getInt("auction_id"));
        }
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi trích lọc mảng số nguyên Auction ID của User ID: " + userId, e);
      throw new DatabaseAccessException("Lấy danh sách ID phiên thắng cuộc thất bại", e);
    }
    return auctionIds;
  }
}
