package org.example.server.daos;

import org.example.core.dto.paymentDTO.PaidHistoryDTO;
import org.example.core.dto.paymentDTO.PendingPaymentsDTO;
import org.example.core.models.entities.BidTransaction;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;
import org.example.core.shared.enums.AuctionStatus;
import org.example.server.config.DBConnection;
import org.example.core.models.entities.Auction;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {
  private static volatile AuctionDAO instance;
  private static ItemDAO itemDAO = ItemDAO.getInstance();

  private AuctionDAO() {}

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

  // Lấy tất cả các phiên đấu giá (Không phân biệt trạng thái - Dành cho Admin)
  public List<Auction> getAllAuctions() {
    List<Auction> auctions = new ArrayList<>();

    // Câu SQL này tôi copy từ hàm getAllAuctionsByStatus của ông,
    // nhưng ĐÃ XÓA dòng "WHERE a.status = ?" để kéo toàn bộ dữ liệu lên.
    String sql =
            "SELECT a.*, "
                    + "COALESCE(MAX(b.bid_amount), i.start_price) AS highest_price "
                    + "FROM auction a "
                    + "JOIN items i ON a.items_id = i.items_id "
                    + "LEFT JOIN bid b ON a.auction_id = b.auction_id "
                    + "GROUP BY a.auction_id";

    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {

      ResultSet rs = ps.executeQuery();

      while (rs.next()) {
        Auction auction = new Auction();
        auction.setAuctionId(rs.getInt("auction_id"));
        auction.setItemId(rs.getInt("items_id"));

        // Bọc check null cho thời gian phòng trường hợp có phiên nháp chưa set giờ
        if (rs.getTimestamp("start_time") != null) {
          auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        }
        if (rs.getTimestamp("end_time") != null) {
          auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        }

        auction.setHighestBid(rs.getBigDecimal("highest_price"));
        auction.setBidIncrement(rs.getBigDecimal("bid_increment"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
          auction.setStatus(AuctionStatus.valueOf(statusStr.toUpperCase()));
        }

        auctions.add(auction);
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return auctions;
  }

  public List<Auction> getAllAuctionsByStatus(AuctionStatus status) {
    List<Auction> auctions = new ArrayList<>();
    String sql =
        "SELECT a.*, i.items_name, i.owner_id, "
            + "COALESCE(MAX(b.bid_amount), i.start_price) AS highest_price "
            + "FROM auction a "
            + "JOIN items i ON a.items_id = i.items_id "
            + "LEFT JOIN bid b ON a.auction_id = b.auction_id "
            + "WHERE a.status = ? "
            + "GROUP BY a.auction_id";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, String.valueOf(status));
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        Auction auction = new Auction();
        auction.setAuctionId(rs.getInt("auction_id"));
        auction.setItemId(rs.getInt("items_id"));
        auction.setItemName(rs.getString("items_name"));
        auction.setOwnerId(rs.getInt("owner_id"));
        auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        auction.setHighestBid(rs.getBigDecimal("highest_price"));
        auction.setBidIncrement(rs.getBigDecimal("bid_increment"));
        auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        auctions.add(auction);
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return auctions;
  }

  public List<Auction> getAllAuctionsByStatusForCatalog(AuctionStatus status) {
    List<Auction> auctions = new ArrayList<>();

    // 1. Cập nhật câu SQL: Lấy thêm tên, loại, và giá khởi điểm của Item
    String sql =
        "SELECT a.auction_id, a.items_id, a.start_time, a.end_time, a.status, a.bid_increment, a.bidder_id,"
            + "i.items_name AS item_name, i.type AS item_type, i.start_price, i.image, i.description, i.owner_id,"
            + "COALESCE(MAX(b.bid_amount), i.start_price) AS highest_price "
            + "FROM auction a "
            + "JOIN items i ON a.items_id = i.items_id "
            + "LEFT JOIN bid b ON a.auction_id = b.auction_id "
            + "WHERE a.status = ? "
            + "GROUP BY a.auction_id, a.items_id, a.start_time, a.end_time, a.status, "
            + "i.items_name, i.type, i.start_price, i.image, i.description, i.owner_id";

    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {

      ps.setString(1, String.valueOf(status));
      ResultSet rs = ps.executeQuery();

      while (rs.next()) {
        Auction auction = new Auction();
        auction.setAuctionId(rs.getInt("auction_id"));
        auction.setItemId(rs.getInt("items_id"));
        auction.setBidderId(rs.getInt("bidder_id"));
        String statusStr = rs.getString("status");
        auction.setOwnerId(rs.getInt("owner_id"));
        if (statusStr != null) {
          auction.setStatus(AuctionStatus.valueOf(statusStr.toUpperCase()));
        }
        auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        auction.setHighestBid(rs.getBigDecimal("highest_price"));
        auction.setBidIncrement(rs.getBigDecimal("bid_increment"));
        auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        List<BidTransaction> bidTransactions =
            BidDAO.getInstance().getBidHistoryByAuctionId(auction.getAuctionId());
        auction.setBidHistory(bidTransactions);

        // 2. Bóc tách dữ liệu Item và khởi tạo object đa hình
        String itemType = rs.getString("item_type");
        Item item = null;

        if (itemType != null) {
          switch (itemType.toUpperCase()) {
            case "ART" -> item = new ArtItem();
            case "ELECTRONICS" -> item = new ElectronicsItem();
            case "VEHICLE" -> item = new VehicleItem();
              // Nếu sau này có thêm loại nào thì add case vào đây
          }

          // Set các thuộc tính chung của Item
          if (item != null) {
            item.setItemId(rs.getInt("items_id"));
            item.setItemName(rs.getString("item_name"));
            item.setType(itemType);
            item.setStartingPrice(rs.getBigDecimal("start_price"));
            item.setImage(rs.getString("image"));
            item.setDescription(rs.getString("description"));

            // Gắn Item hoàn chỉnh vào Auction
            auction.setItem(item);
          }
        }

        auctions.add(auction);
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }

    return auctions;
  }

  public List<PendingPaymentsDTO> getAllAuctionsFinished(int userId) {
    List<org.example.core.dto.paymentDTO.PendingPaymentsDTO> pendingPaymentsDTOs =
        new ArrayList<>();
    String sql =
        "SELECT auction_id, items_id, highest_price, end_time FROM auction WHERE status = 'FINISHED' AND bidder_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        org.example.core.dto.paymentDTO.PendingPaymentsDTO pendingPaymentsDTO =
            new org.example.core.dto.paymentDTO.PendingPaymentsDTO();
        pendingPaymentsDTO.setAuctionId(rs.getInt("auction_id"));
        pendingPaymentsDTO.setItemName(itemDAO.getItemNameByItemId(rs.getInt("items_id")));
        pendingPaymentsDTO.setWinPrice(rs.getBigDecimal("highest_price"));
        pendingPaymentsDTO.setEndDate(rs.getTimestamp("end_time").toLocalDateTime().plusHours(24));
        pendingPaymentsDTOs.add(pendingPaymentsDTO);
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return pendingPaymentsDTOs;
  }

  public List<PaidHistoryDTO> getAllAuctionsPaid(int userId) {
    List<PaidHistoryDTO> paidHistoryDTOs = new ArrayList<>();
    String sql =
        """
    SELECT i.items_name, i.type AS category,
           a.highest_price AS final_price,
           w.created_at AS paid_date
    FROM auction a
    JOIN wallet_transaction w ON a.auction_id = w.reference_id
    JOIN items i ON a.items_id = i.items_id
    WHERE a.status = 'PAID'
    AND a.bidder_id = ?
    AND w.transaction_type = 'PAY_AUCTION'""";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        PaidHistoryDTO paidHistoryDTO = new PaidHistoryDTO();
        paidHistoryDTO.setCategory(rs.getString("category"));
        paidHistoryDTO.setPaidDate(rs.getTimestamp("paid_date").toLocalDateTime());
        paidHistoryDTO.setFinalPrice(rs.getBigDecimal("final_price"));
        paidHistoryDTO.setItemName(rs.getString("items_name"));
        paidHistoryDTOs.add(paidHistoryDTO);
      }
      return paidHistoryDTOs;
    } catch (SQLException | IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public List<Integer> getAllAuctionIdFinishedByUserId(int userId) {
    List<Integer> auctionIds = new ArrayList<>();
    String sql = "SELECT auction_id FROM auction WHERE bidder_id = ? AND status = 'FINISHED'";

    try (Connection conn = DBConnection.getConnection(); // Sử dụng class kết nối DB của bạn
        PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setInt(1, userId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          auctionIds.add(rs.getInt("auction_id"));
        }
      }
    } catch (SQLException | IOException e) {
      System.err.println("Lỗi khi lấy danh sách Auction ID: " + e.getMessage());
      e.printStackTrace();
    }

    return auctionIds;
  }

  public int createNewAuctionItem(
      Item item, long time, BigDecimal bidIncrement, LocalDateTime startTime) {
    String sql =
        "INSERT INTO auction (items_id, start_price, bid_increment, end_time, start_time) VALUES (?,?,?,?,?)";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, item.getItemId());
      ps.setBigDecimal(2, item.getStartingPrice());
      ps.setBigDecimal(3, bidIncrement);
      LocalDateTime endtime = startTime.plusMinutes(time);
      ps.setTimestamp(4, Timestamp.valueOf(endtime));
      ps.setTimestamp(5, Timestamp.valueOf(startTime));
      try (ResultSet rs = ps.executeUpdate() > 0 ? ps.getGeneratedKeys() : null) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return -1;
  }

  public void setAuctionStatus(int auctionId, AuctionStatus status) {
    String sql = "UPDATE auction SET status = ? WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, status.name());
      ps.setInt(2, auctionId);
      ps.executeUpdate();
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

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
          if (startTs != null) {
            auction.setStartTime(startTs.toLocalDateTime());
          }

          Timestamp endTs = rs.getTimestamp("end_time");
          if (endTs != null) {
            auction.setEndTime(endTs.toLocalDateTime());
          }

          auction.setBidIncrement(rs.getBigDecimal("bid_increment"));
          auction.setHighestBid(rs.getBigDecimal("highest_price"));
          auction.setBidderId(rs.getInt("bidder_id"));

          // 🛡️ BẢO HIỂM 1: Tự động tính khoảng cách số phút thực tế từ DB để durationMinutes không bao giờ bằng 0
          if (auction.getStartTime() != null && auction.getEndTime() != null) {
            long minutes = java.time.Duration.between(auction.getStartTime(), auction.getEndTime()).toMinutes();
            auction.setDurationMinutes(minutes);
          }

          // 🛡️ BẢO HIỂM 2: Nạp kèm luôn đối tượng sản phẩm Item vào để Client mở phòng có dữ liệu render tranh ảnh/mô tả
          try {
            Item item = ItemDAO.getInstance().getItemById(auction.getItemId());
            auction.setItem(item);
          } catch (Exception e) {
            System.err.println("Lỗi nạp kèm Item trong DAO: " + e.getMessage());
          }

          return auction;
        }
      }
    } catch (SQLException | java.io.IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

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
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public boolean updateAuctionEndTime(int auctionId, LocalDateTime endTime) {
    String sql = "UPDATE auction SET end_time = ? WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setTimestamp(1, Timestamp.valueOf(endTime));
      ps.setInt(2, auctionId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String getAuctionStatus(int auctionId) {
    String sql = "SELECT status FROM auction WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("status");
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public boolean updateHighestPriceByItemId(int itemId, BigDecimal newPrice) {
    String sql = "UPDATE auction SET highest_price = ? WHERE items_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setBigDecimal(1, newPrice);
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
