package org.example.server.services;

import org.example.core.dto.userDTO.SellerDashboardDTO;
import org.example.server.daos.DashboardDAO;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Dịch vụ cung cấp và tổng hợp dữ liệu báo cáo thống kê phục vụ nhu cầu kết xuất dữ liệu đồ thị
 * Dashboard.
 */
public class DashBoardService {
  private static final Logger logger = Logger.getLogger(DashBoardService.class.getName());
  private static volatile DashBoardService instance = null;
  private final DashboardDAO dashboardDAO;

  DashBoardService(DashboardDAO dashboardDAO) {
    this.dashboardDAO = dashboardDAO;
  }

  /** Lấy instance duy nhất (Singleton) của DashBoardService (Thread-safe). */
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

  /** Trích xuất tổng hợp bộ chỉ số cốt lõi KPI của sàn đấu giá dành cho Admin. */
  public Map<String, String> getKPIs() throws Exception {
    Map<String, String> kpis = dashboardDAO.getKPIs();
    if (kpis == null || kpis.isEmpty()) {
      throw new Exception(
          "Lỗi hệ thống: Không thể tải hoặc thống kê các số liệu KPI của nền tảng.");
    }
    return kpis;
  }

  /** Trích xuất dữ liệu phân bổ số lượng phòng theo danh mục hàng hóa (PieChart). */
  public Map<String, Integer> getCategoryDistribution() throws Exception {
    Map<String, Integer> data = dashboardDAO.getCategoryDistribution();
    if (data == null) {
      throw new Exception("Lỗi hệ thống: Không thể lấy dữ liệu phân bổ biểu đồ theo danh mục.");
    }
    return data;
  }

  /** Trích xuất báo cáo phân bổ số lượng phòng theo trạng thái sàn đấu giá (BarChart). */
  public Map<String, Integer> getAuctionStatusDistribution() throws Exception {
    Map<String, Integer> statusData = dashboardDAO.getAuctionStatusDistribution();
    if (statusData == null) {
      throw new Exception(
          "Lỗi hệ thống: Không thể kết xuất báo cáo phân bổ trạng thái các phiên đấu giá.");
    }
    return statusData;
  }

  /** Thu thập toàn bộ chỉ số kinh doanh, tăng trưởng doanh số lũy kế của riêng một Người bán. */
  public SellerDashboardDTO getSellerDashboard(int sellerId) throws Exception {
    if (sellerId <= 0) {
      throw new Exception("Mã người bán không hợp lệ để trích xuất báo cáo doanh thu cá nhân!");
    }
    SellerDashboardDTO dto = dashboardDAO.getSellerDashboardStats(sellerId);
    if (dto == null) {
      throw new Exception(
          "Không tìm thấy dữ liệu thống kê hoặc người bán chưa có hoạt động kinh doanh nào.");
    }
    return dto;
  }
}
