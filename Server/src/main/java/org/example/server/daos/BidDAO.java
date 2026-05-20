package org.example.server.daos;

import org.example.core.models.entities.BidTransaction;
import org.example.server.config.DBConnection;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {
  private static volatile BidDAO instance;

  private BidDAO() {}

  public static BidDAO getInstance() {
    if (instance == null) {
      synchronized (BidDAO.class) {
        if (instance == null) {
          instance = new BidDAO();
        }
      }
    }
    return instance;
  }

  /**
   * Hàm ghi nhận lượt đặt thầu chuẩn OOP:
   * Tiếp nhận đối tượng thực thể đã đóng gói trọn gói từ tầng Service!
   */
  public boolean insertBid(BidTransaction tx) {
    String sql = "INSERT INTO bid (auction_id, bidder_id, bid_amount, user_name) VALUES (?, ?, ?, ?)";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, tx.getAuctionId());   // Sử dụng đúng các getter của thực thể Core
      ps.setInt(2, tx.getBidderId());
      ps.setBigDecimal(3, tx.getAmount());
      ps.setString(4, tx.getBidderName());

      int rowsUpdated = ps.executeUpdate();
      return rowsUpdated > 0;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /** Lấy giá hiện tại của phiên từ bảng auction_items Nếu chưa có -> trả null */
  public BigDecimal getCurrentPrice(int auctionId) {
    String sql = "SELECT highest_price FROM auction WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getBigDecimal("highest_price");
        }
        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /** Cập nhật current_price sau khi nhận bid hợp lệ */
  public boolean updateCurrentPrice(int auctionId, int bidderId, BigDecimal newPrice) {
    String sql = "UPDATE auction SET highest_price = ?, bidder_id = ? WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setBigDecimal(1, newPrice);
      ps.setInt(2, bidderId);
      ps.setInt(3, auctionId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public int getBidIdByItemsId(int itemId) {
    String sql =
        "SELECT b.bid_id "
            + "FROM bid b "
            + "JOIN auction ai ON b.auction_id = ai.auction_id "
            + "WHERE ai.items_id = ? "
            + "ORDER BY b.bid_id DESC "
            + "LIMIT 1";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getInt("bid_id");
        return 0;
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Lịch sử bid theo auction để FE vẽ biểu đồ */
  public List<BidTransaction> getBidHistoryByAuctionId(int auctionId) {
    List<BidTransaction> transactions = new ArrayList<>();
    String sql =
        "SELECT bid_amount, created_at, bidder_id FROM bid WHERE auction_id = ? ORDER BY created_at ASC";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          BigDecimal amount = rs.getBigDecimal("bid_amount");
          Timestamp ts = rs.getTimestamp("created_at");
          int bidderId = rs.getInt("bidder_id");
          String bidderName = UserDAO.getInstance().getUserNameByUserId(bidderId);
          LocalDateTime time = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();

          transactions.add(new BidTransaction(amount, time, bidderId, bidderName));
        }
      }
      return transactions;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public List<BidTransaction> getBidTransactionByUserId(int userId) {
    List<BidTransaction> transactions = new ArrayList<>();
    String sql =
        "SELECT bid_amount, created_at, bidder_id FROM bid WHERE bidder = ? ORDER BY created_at DESC";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          BigDecimal amount = rs.getBigDecimal("bid_amount");
          Timestamp ts = rs.getTimestamp("created_at");
          int bidderId = rs.getInt("bidder_id");
          String bidderName = UserDAO.getInstance().getUserNameByUserId(bidderId);
          LocalDateTime time = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();

          transactions.add(new BidTransaction(amount, time, bidderId, bidderName));
        }
      }
      return transactions;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
