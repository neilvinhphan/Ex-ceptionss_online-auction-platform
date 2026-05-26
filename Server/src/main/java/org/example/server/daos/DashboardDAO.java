package org.example.server.daos;

import org.example.core.dto.userDTO.SellerDashboardDTO;
import org.example.core.exception.DatabaseAccessException;
import org.example.server.config.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lớp truy cập dữ liệu (DAO) xử lý các truy vấn thống kê, tổng hợp dữ liệu (KPIs, Charts) cho Admin và Seller.
 */
public class DashboardDAO {
    private static final Logger logger = Logger.getLogger(DashboardDAO.class.getName());
    private static volatile DashboardDAO instance;

    private DashboardDAO() {}

    public static DashboardDAO getInstance() {
        if (instance == null) {
            synchronized (DashboardDAO.class) {
                if (instance == null) {
                    instance = new DashboardDAO();
                }
            }
        }
        return instance;
    }

    /** Lấy bộ chỉ số KPI tổng quan hệ thống hiển thị trên màn hình Admin Dashboard. */
    public Map<String, String> getKPIs() {
        Map<String, String> kpis = new HashMap<>();

        // 1. Đếm tổng số lượng User (Trừ Admin)
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM user WHERE role != 'ADMIN'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) kpis.put("totalUsers", String.valueOf(rs.getInt(1)));
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Lỗi đếm tổng số User trong hệ thống", e);
            kpis.put("totalUsers", "0");
        }

        // 2. Đếm số lượng phiên đấu giá đang chạy (RUNNING)
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM auction WHERE status = 'RUNNING'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) kpis.put("activeAuctions", String.valueOf(rs.getInt(1)));
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Lỗi đếm số phiên đấu giá đang hoạt động", e);
            kpis.put("activeAuctions", "0");
        }

        // 3. Đếm số lượng tài sản đang chờ phê duyệt (PENDING)
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM items WHERE status = 'PENDING'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) kpis.put("pendingCount", String.valueOf(rs.getInt(1)));
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Lỗi đếm số tài sản chờ duyệt", e);
            kpis.put("pendingCount", "0");
        }

        // 4. Tính tổng doanh thu hệ thống (Chỉ cộng dồn các phiên đã thanh toán - PAID)
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(highest_price) FROM auction WHERE status = 'PAID'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal(1);
                if (total == null) total = BigDecimal.ZERO;
                kpis.put("totalVolume", String.format("%,.0f", total));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Lỗi tổng hợp tổng doanh thu hệ thống", e);
            kpis.put("totalVolume", "0");
        }

        return kpis;
    }

    /** Thống kê tỷ lệ phân bổ sản phẩm theo từng ngành hàng phục vụ vẽ biểu đồ tròn (PieChart). */
    public Map<String, Integer> getCategoryDistribution() {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT i.type, COUNT(a.auction_id) FROM auction a JOIN items i ON a.items_id = i.items_id GROUP BY i.type";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thống kê phân bổ ngành hàng", e);
            throw new DatabaseAccessException("Tải dữ liệu biểu đồ phân bổ ngành hàng thất bại.", e);
        }
        return map;
    }

    /** Thống kê số lượng phiên đấu giá theo từng trạng thái phục vụ vẽ biểu đồ cột (BarChart). */
    public Map<String, Integer> getAuctionStatusDistribution() {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT status, COUNT(auction_id) FROM auction GROUP BY status";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thống kê trạng thái phiên đấu giá", e);
            throw new DatabaseAccessException("Tải dữ liệu biểu đồ trạng thái phiên đấu giá thất bại.", e);
        }
        return map;
    }

    /** Tổng hợp toàn bộ dữ liệu thống kê của riêng một Người bán (Seller). */
    public SellerDashboardDTO getSellerDashboardStats(int sellerId) {
        double totalRevenue = 0;
        int totalSold = 0;
        Map<String, Integer> categories = new HashMap<>();
        Map<String, Double> revenueGrowth = new LinkedHashMap<>();

        String sqlKpi = "SELECT SUM(amount), COUNT(transaction_id) FROM wallet_transaction WHERE user_id = ? AND transaction_type = 'SELL_REVENUE'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlKpi)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalRevenue = rs.getDouble(1);
                    totalSold = rs.getInt(2);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy dữ liệu KPI người bán cho Seller ID: " + sellerId, e);
            throw new DatabaseAccessException("Tải dữ liệu KPI thống kê của người bán thất bại.", e);
        }

        String sqlPie = """
            SELECT i.type, COUNT(wt.reference_id) 
            FROM wallet_transaction wt
            JOIN auction a ON wt.reference_id = a.auction_id
            JOIN items i ON a.items_id = i.items_id
            WHERE wt.user_id = ? AND wt.transaction_type = 'SELL_REVENUE'
            GROUP BY i.type
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlPie)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.put(rs.getString(1), rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy số liệu ngành hàng cho Seller ID: " + sellerId, e);
            throw new DatabaseAccessException("Tải số liệu phân bổ ngành hàng biểu đồ tròn thất bại.", e);
        }

        String sqlLine = """
            SELECT reference_id, amount 
            FROM wallet_transaction 
            WHERE user_id = ? AND transaction_type = 'SELL_REVENUE'
            ORDER BY transaction_id ASC
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlLine)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                int orderCount = 1;
                double cumulativeRevenue = 0;

                while (rs.next()) {
                    String label = "Đơn " + orderCount++;
                    double currentAmount = rs.getDouble("amount");
                    cumulativeRevenue += currentAmount;
                    revenueGrowth.put(label, cumulativeRevenue);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi dựng biểu đồ đường tăng trưởng doanh thu cho Seller ID: " + sellerId, e);
            throw new DatabaseAccessException("Tải lịch sử doanh thu tăng trưởng biểu đồ đường thất bại.", e);
        }

        return new SellerDashboardDTO(totalRevenue, totalSold, categories, revenueGrowth);
    }
}