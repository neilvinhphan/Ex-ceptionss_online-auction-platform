package org.example.server.daos;

import org.example.core.models.entities.BidTransaction;
import org.example.core.exception.DatabaseAccessException;
import org.example.core.exception.ResourceNotFoundException;
import org.example.server.config.DBConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Lớp truy cập dữ liệu (DAO) quản lý các giao dịch đặt giá, đấu thầu (Bid). */
public class BidDAO {
  private static final Logger logger = Logger.getLogger(BidDAO.class.getName());
  private static volatile BidDAO instance;

  private BidDAO() {}

  /** Lấy instance duy nhất (Singleton) của BidDAO dưới cơ chế Thread-safe. */
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

  // --- NHÓM PHƯƠNG THỨC THAY ĐỔI DỮ LIỆU (WRITE) ---

  /**
   * Lưu một bản ghi lịch sử đặt thầu mới (Bid Transaction) từ tầng Service vào cơ sở dữ liệu.
   */
  public boolean insertBid(BidTransaction tx) {
    String sql =
            "INSERT INTO bid (auction_id, bidder_id, bid_amount, user_name) VALUES (?, ?, ?, ?)";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, tx.getAuctionId());
      ps.setInt(2, tx.getBidderId());
      ps.setBigDecimal(3, tx.getAmount());
      ps.setString(4, tx.getBidderName());

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(
              Level.SEVERE,
              "Lỗi khi ghi nhận lượt đặt thầu mới cho Auction ID: " + tx.getAuctionId(),
              e);
      throw new DatabaseAccessException("Ghi nhận lượt đặt giá mới thất bại do sự cố đồng bộ dữ liệu.", e);
    }
  }

  /** Cập nhật lại cột giá cao nhất hiện tại và mã định danh Winner của phòng đấu giá. */
  public boolean updateCurrentPrice(int auctionId, int bidderId, BigDecimal newPrice) {
    String sql = "UPDATE auction SET highest_price = ?, bidder_id = ? WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setBigDecimal(1, newPrice);
      ps.setInt(2, bidderId);
      ps.setInt(3, auctionId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi cập nhật bước giá mới cho phòng đấu giá ID: " + auctionId, e);
      throw new DatabaseAccessException("Cập nhật giá hiện tại của phiên đấu giá thất bại.", e);
    }
  }

  // --- NHÓM PHƯƠNG THỨC TRUY VẤN DỮ LIỆU (READ) ---

  /** Lấy số tiền đấu giá cao nhất hiện tại ở bảng Auction. */
  public BigDecimal getCurrentPrice(int auctionId) {
    String sql = "SELECT highest_price FROM auction WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getBigDecimal("highest_price");
        }
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi khi lấy giá hiện tại của Auction ID: " + auctionId, e);
      throw new DatabaseAccessException("Truy vấn giá hiện tại của phiên thất bại.", e);
    }
    throw new ResourceNotFoundException("Không tìm thấy thông tin giá cho phiên đấu giá ID: " + auctionId);
  }

  /**
   * Truy xuất toàn bộ danh sách lịch sử đặt thầu của một phòng phục vụ việc vẽ đồ thị Client.
   */
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
      logger.log(Level.SEVERE, "Lỗi truy xuất lịch sử đặt thầu của Auction ID: " + auctionId, e);
      throw new DatabaseAccessException("Tải lịch sử đặt giá của phiên thất bại.", e);
    }
  }
}