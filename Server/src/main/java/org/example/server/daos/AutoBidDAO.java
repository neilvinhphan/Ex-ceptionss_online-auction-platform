package org.example.server.daos;

import org.example.server.config.DBConnection;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAO {
    private static volatile AutoBidDAO instance;

    private AutoBidDAO() {}

    public static synchronized AutoBidDAO getInstance() {
        if (instance == null) instance = new AutoBidDAO();
        return instance;
    }

    // 1. Lưu hoặc Cập nhật giá trần của Bot (Nếu đã tồn tại thì đè giá mới lên và bật lại trạng thái hoạt động)
    public void saveOrUpdateAutoBid(int auctionId, int userId, BigDecimal maxBid) {
        String sql = "INSERT INTO autobid_configs (auction_id, user_id, max_bid, is_active) VALUES (?, ?, ?, TRUE) " +
                "ON DUPLICATE KEY UPDATE max_bid = ?, is_active = TRUE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, userId);
            ps.setBigDecimal(3, maxBid);
            ps.setBigDecimal(4, maxBid);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();}
    }

    // 2. Tắt trạng thái hoạt động của Bot (Khi người dùng bấm nút Hủy gác phòng giữa chừng)
    public void disableAutoBid(int auctionId, int userId) {
        String sql = "UPDATE autobid_configs SET is_active = FALSE WHERE auction_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();}
    }

    // 3. Lấy danh sách Bot đang hoạt động trong phòng (Sắp xếp ưu tiên: Giá trần cao nhất lên đầu, Thời gian cài trước xếp trước)
    public List<AutoBidConfig> getActiveAutoBidsForAuction(int auctionId) throws SQLException {
        List<AutoBidConfig> list = new ArrayList<>();
        String sql = "SELECT * FROM autobid_configs WHERE auction_id = ? AND is_active = TRUE ORDER BY max_bid DESC, created_at ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AutoBidConfig config = new AutoBidConfig(
                            rs.getInt("autobid_id"),
                            rs.getInt("auction_id"),
                            rs.getInt("user_id"),
                            rs.getBigDecimal("max_bid"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                    list.add(config);
                }
            }
        }
        return list;
    }

    // Chèn thêm vào AutoBidDAO.java
    public BigDecimal getMaxAutoBid(int auctionId, int userId) {
        String sql = "SELECT max_bid FROM autobid_configs WHERE auction_id = ? AND user_id = ? AND is_active = TRUE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("max_bid"); // Trả về mức giá trần đang gác phòng
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();}
        return null; // Không có bot nào đang chạy
    }

    // --- LỚP TRỢ THỦ (POJO) CHỨA CẤU HÌNH ĐỂ SERVER TÍNH TOÁN TOÁN HỌC ---
    public static class AutoBidConfig {
        private int autoBidId;
        private int auctionId;
        private int userId;
        private BigDecimal maxBid;
        private LocalDateTime createdAt;

        public AutoBidConfig(int autoBidId, int auctionId, int userId, BigDecimal maxBid, LocalDateTime createdAt) {
            this.autoBidId = autoBidId;
            this.auctionId = auctionId;
            this.userId = userId;
            this.maxBid = maxBid;
            this.createdAt = createdAt;
        }

        public AutoBidConfig() {}

        public int getAutoBidId() { return autoBidId; }
        public int getAuctionId() { return auctionId; }
        public int getUserId() { return userId; }
        public BigDecimal getMaxBid() { return maxBid; }
        public LocalDateTime getCreatedAt() { return createdAt; }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public void setMaxBid(BigDecimal maxBid) {
            this.maxBid = maxBid;
        }
    }
}