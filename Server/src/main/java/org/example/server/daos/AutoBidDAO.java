package org.example.server.daos;

import org.example.core.exception.DataConflictException;
import org.example.core.exception.DatabaseAccessException;
import org.example.server.config.DBConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Lớp truy cập dữ liệu (DAO) quản lý cấu hình đấu giá tự động (Bot gác phòng / AutoBid). */
public class AutoBidDAO {
  private static final Logger logger = Logger.getLogger(AutoBidDAO.class.getName());
  private static volatile AutoBidDAO instance;

  private AutoBidDAO() {}

  /** Lấy instance duy nhất (Singleton) của AutoBidDAO. */
  public static synchronized AutoBidDAO getInstance() {
    if (instance == null) {
      instance = new AutoBidDAO();
    }
    return instance;
  }

  // --- NHÓM PHƯƠNG THỨC THAY ĐỔI DỮ LIỆU (WRITE) ---

  /** Lưu cấu hình hoặc nâng giá trần đặt tự động của một tài khoản trong phòng. */
  public void saveOrUpdateAutoBid(int auctionId, int userId, BigDecimal maxBid) {
    String sql =
        "INSERT INTO autobid_configs (auction_id, user_id, max_bid, is_active) VALUES (?, ?, ?, TRUE) "
            + "ON DUPLICATE KEY UPDATE max_bid = ?, is_active = TRUE";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      ps.setInt(2, userId);
      ps.setBigDecimal(3, maxBid);
      ps.setBigDecimal(4, maxBid);
      ps.executeUpdate();
    } catch (SQLException e) {
      logger.log(
          Level.SEVERE, "Lỗi lưu/cập nhật AutoBid cho phòng ID: " + auctionId + ", User: " + userId, e);
      if(e.getErrorCode() == 1062) {
      throw new DataConflictException("Không thể thiết lập cấu hình đấu giá tự động");
    }
    throw new DatabaseAccessException("Lỗi không xác định khi lưu cấu hình đấu giá tự động", e);
  }}

  /** Hủy chế độ đặt giá tự động (Tắt hoạt động bot) khi người dùng chủ động thoát phòng gác. */
  public void disableAutoBid(int auctionId, int userId) {
    String sql =
        "UPDATE autobid_configs SET is_active = FALSE WHERE auction_id = ? AND user_id = ?";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      ps.setInt(2, userId);
      ps.executeUpdate();
    } catch (SQLException e) {
      logger.log(
          Level.SEVERE,
          "Lỗi tắt trạng thái AutoBid phòng ID: " + auctionId + ", User: " + userId,
          e);
      throw new DatabaseAccessException("Không thể hủy trạng thái đấu giá tự động", e);
    }
  }

  // --- NHÓM PHƯƠNG THỨC TRUY VẤN DỮ LIỆU (READ) ---

  /** Lấy mức giá trần gác phòng tự động đang hoạt động của người dùng cụ thể. */
  public BigDecimal getMaxAutoBid(int auctionId, int userId) {
    String sql =
        "SELECT max_bid FROM autobid_configs WHERE auction_id = ? AND user_id = ? AND is_active = TRUE";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      ps.setInt(2, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getBigDecimal("max_bid");
        }
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi truy vấn mức giá trần AutoBid phòng ID: " + auctionId, e);
      throw new DatabaseAccessException("Lấy hạn mức đấu giá tự động thất bại", e);
    }
    return null;
  }

  /**
   * Lấy toàn bộ danh sách Bot AutoBid đang chạy trong một phòng đấu giá. Thứ tự ưu tiên: Giá trần
   * cao nhất lên trước, thiết lập sớm xếp trước.
   */
  public List<AutoBidConfig> getActiveAutoBidsForAuction(int auctionId) throws SQLException {
    List<AutoBidConfig> list = new ArrayList<>();
    String sql =
        "SELECT * FROM autobid_configs WHERE auction_id = ? AND is_active = TRUE ORDER BY max_bid DESC, created_at ASC";
    try (Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          AutoBidConfig config =
              new AutoBidConfig(
                  rs.getInt("autobid_id"),
                  rs.getInt("auction_id"),
                  rs.getInt("user_id"),
                  rs.getBigDecimal("max_bid"),
                  rs.getTimestamp("created_at").toLocalDateTime());
          list.add(config);
        }
      }
    } catch (SQLException e) {
      logger.log(
          Level.SEVERE,
          "Lỗi trích xuất danh sách các bot đang chạy trong phòng ID: " + auctionId,
          e);
      throw e;
    }
    return list;
  }

  // --- LỚP TRỢ THỦ (POJO / NESTED CLASS) ---

  /** Thực thể lưu trữ cấu hình thông số kỹ thuật của một thiết lập AutoBid. */
  public static class AutoBidConfig {
    private int autoBidId;
    private int auctionId;
    private int userId;
    private BigDecimal maxBid;
    private LocalDateTime createdAt;

    public AutoBidConfig() {}

    public AutoBidConfig(
        int autoBidId, int auctionId, int userId, BigDecimal maxBid, LocalDateTime createdAt) {
      this.autoBidId = autoBidId;
      this.auctionId = auctionId;
      this.userId = userId;
      this.maxBid = maxBid;
      this.createdAt = createdAt;
    }

    public int getUserId() {
      return userId;
    }

    public BigDecimal getMaxBid() {
      return maxBid;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setUserId(int userId) {
      this.userId = userId;
    }

    public void setMaxBid(BigDecimal maxBid) {
      this.maxBid = maxBid;
    }
  }
}
