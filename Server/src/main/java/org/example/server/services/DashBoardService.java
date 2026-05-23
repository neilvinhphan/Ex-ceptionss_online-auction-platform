package org.example.server.services;

import org.example.core.dto.userDTO.SellerDashboardDTO;
import org.example.server.daos.DashboardDAO;

import java.util.Map;

public class DashBoardService {
    private final DashboardDAO dashboardDAO;
    private static volatile DashBoardService instance = null;

    DashBoardService(DashboardDAO dashboardDAO) {
        this.dashboardDAO = dashboardDAO;
    }

    public static DashBoardService getInstance() {
        if (instance == null) {
            synchronized (DashBoardService.class) {
                if (instance == null) {
                    instance = new DashBoardService(DashboardDAO.getInstance());
                }
            }
        }
        return instance;
    }

    public Map<String, String> getKPIs() throws Exception {
        Map<String, String> kpis = dashboardDAO.getKPIs();
        if (kpis == null || kpis.isEmpty()) {
            throw new Exception("Lỗi hệ thống: Không thể tải hoặc thống kê các số liệu KPI của nền tảng.");
        }
        return kpis;
    }

    public Map<String, Integer> getCategoryDistribution() throws Exception {
        Map<String, Integer> data = dashboardDAO.getCategoryDistribution();
        if (data == null) {
            throw new Exception("Lỗi hệ thống: Không thể lấy dữ liệu phân bổ biểu đồ theo danh mục.");
        }
        return data;
    }

    public Map<String, Integer> getAuctionStatusDistribution() throws Exception {
        Map<String, Integer> statusData = dashboardDAO.getAuctionStatusDistribution();
        if (statusData == null) {
            throw new Exception("Lỗi hệ thống: Không thể kết xuất báo cáo phân bổ trạng thái các phiên đấu giá.");
        }
        return statusData;
    }

    public SellerDashboardDTO getSellerDashboard(int sellerId) throws Exception {
        if (sellerId <= 0) {
            throw new Exception("Mã người bán không hợp lệ để trích xuất báo cáo doanh thu cá nhân!");
        }
        SellerDashboardDTO dto = dashboardDAO.getSellerDashboardStats(sellerId);
        if (dto == null) {
            throw new Exception("Không tìm thấy dữ liệu thống kê hoặc người bán chưa có hoạt động kinh doanh nào.");
        }
        return dto;
    }
}