package org.example.server.daos;

import org.example.server.config.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import org.example.core.dto.userDTO.SellerDashboardDTO;

public class DashboardDAO {
    private static volatile DashboardDAO instance;

    private DashboardDAO() {}

    public static DashboardDAO getInstance() {
        if (instance == null) {
            instance = new DashboardDAO();
        }
        return instance;
    }

    // 1. LẤY BỘ CHỈ SỐ KPI
    public Map<String, String> getKPIs() {
        Map<String, String> kpis = new HashMap<>();

        // Đếm tổng User
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM user WHERE role != 'ADMIN'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) kpis.put("totalUsers", String.valueOf(rs.getInt(1)));
        } catch (Exception e) { kpis.put("totalUsers", "0"); }

        // Đếm phiên đang chạy (RUNNING)
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM auction WHERE status = 'RUNNING'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) kpis.put("activeAuctions", String.valueOf(rs.getInt(1)));
        } catch (Exception e) { kpis.put("activeAuctions", "0"); }

        // Đếm tài sản chờ duyệt (DAFT)
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM items WHERE status = 'PENDING'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) kpis.put("pendingCount", String.valueOf(rs.getInt(1)));
        } catch (Exception e) { kpis.put("pendingCount", "0"); }

        // Tính tổng doanh thu (Chỉ tính các phiên đã PAID)
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(highest_price) FROM auction WHERE status = 'PAID'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal(1);
                if (total == null) total = BigDecimal.ZERO;
                // Format luôn thành chuỗi có dấu phẩy cho đẹp giống hợp đồng
                kpis.put("totalVolume", String.format("%,.0f", total));
            }
        } catch (Exception e) { kpis.put("totalVolume", "0"); }

        return kpis;
    }

    // 2. THỐNG KÊ NGÀNH HÀNG (Dành cho PieChart)
    public Map<String, Integer> getCategoryDistribution() {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT i.type, COUNT(a.auction_id) FROM auction a JOIN items i ON a.items_id = i.items_id GROUP BY i.type";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return map;
    }

    // 3. THỐNG KÊ TRẠNG THÁI PHIÊN (Dành cho BarChart)
    public Map<String, Integer> getAuctionStatusDistribution() {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT status, COUNT(auction_id) FROM auction GROUP BY status";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return map;
    }

    // LẤY THỐNG KÊ DOANH THU CHO NGƯỜI BÁN
    public SellerDashboardDTO getSellerDashboardStats(int sellerId) {
        double totalRevenue = 0;
        int totalSold = 0;
        Map<String, Integer> categories = new HashMap<>();
        // Sử dụng LinkedHashMap để giữ đúng thứ tự đơn hàng từ cũ đến mới khi vẽ biểu đồ
        Map<String, Double> revenueGrowth = new java.util.LinkedHashMap<>();

        // 1. Lấy dữ liệu KPI
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
        } catch (Exception e) { e.printStackTrace(); }

        // 2. Lấy số liệu Biểu đồ tròn (Ngành hàng)
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
        } catch (Exception e) { e.printStackTrace(); }

        // 3. BỔ SUNG: Lấy số liệu Biểu đồ đường (Tăng trưởng doanh thu qua từng đơn)
        String sqlLine = """
        SELECT reference_id, amount 
        FROM wallet_transaction 
        WHERE user_id = ? AND transaction_type = 'SELL_REVENUE'
        ORDER BY transaction_id ASC
    """; // Bạn có thể đổi thành ORDER BY created_at ASC nếu bảng có cột thời gian tạo

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlLine)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                int orderCount = 1;
                double cumulativeRevenue = 0; // Biến tích lũy để tính doanh thu cộng dồn (tăng trưởng)

                while (rs.next()) {
                    // Định dạng nhãn trục X (Ví dụ: "Đơn 1", "Đơn 2",...) để biểu đồ gọn gàng
                    String label = "Đơn " + orderCount++;

                    // HOẶC nếu muốn hiển thị trực tiếp mã đơn hàng, bạn dùng dòng dưới:
                    // String label = "ĐH-" + rs.getString("reference_id");

                    double currentAmount = rs.getDouble("amount");
                    cumulativeRevenue += currentAmount; // Cộng dồn doanh thu đơn này vào tổng từ trước tới giờ

                    // LỰA CHỌN HIỂN THỊ:
                    // - Để vẽ đường doanh thu LŨY KẾ tăng dần: truyền `cumulativeRevenue` (Khuyên dùng cho từ "Tăng trưởng")
                    // - Để vẽ doanh thu RIÊNG LẺ của từng đơn: truyền `currentAmount`
                    revenueGrowth.put(label, cumulativeRevenue);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        // 4. Khởi tạo DTO và set map dữ liệu tăng trưởng doanh thu vào
        SellerDashboardDTO dto = new SellerDashboardDTO(totalRevenue, totalSold, categories, revenueGrowth);
        return dto;
    }
}