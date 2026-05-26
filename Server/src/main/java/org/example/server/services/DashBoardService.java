package org.example.server.services;

import org.example.core.dto.userDTO.SellerDashboardDTO;
import org.example.core.exception.ResourceNotFoundException;
import org.example.core.exception.InvalidUserDataException;
import org.example.server.daos.DashboardDAO;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Dịch vụ cung cấp và tổng hợp dữ liệu báo cáo thống kê phục vụ nhu cầu kết xuất dữ liệu đồ thị.
 */
public class DashBoardService {
  private static final Logger logger = Logger.getLogger(DashBoardService.class.getName());
  private static volatile DashBoardService instance = null;
  private final DashboardDAO dashboardDAO;

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

  public Map<String, String> getKPIs() {
    Map<String, String> kpis = dashboardDAO.getKPIs();
    if (kpis == null || kpis.isEmpty()) {
      throw new ResourceNotFoundException("Lỗi hệ thống: Không thể tải các số liệu KPI của nền tảng.");
    }
    return kpis;
  }

  public Map<String, Integer> getCategoryDistribution() {
    Map<String, Integer> data = dashboardDAO.getCategoryDistribution();
    if (data == null) {
      throw new ResourceNotFoundException("Lỗi hệ thống: Không thể lấy dữ liệu phân bổ theo danh mục.");
    }
    return data;
  }

  public Map<String, Integer> getAuctionStatusDistribution() {
    Map<String, Integer> statusData = dashboardDAO.getAuctionStatusDistribution();
    if (statusData == null) {
      throw new ResourceNotFoundException("Lỗi hệ thống: Không thể kết xuất báo cáo phân bổ trạng thái.");
    }
    return statusData;
  }

  public SellerDashboardDTO getSellerDashboard(int sellerId) {
    if (sellerId <= 0) {
      throw new InvalidUserDataException("Mã người bán không hợp lệ để trích xuất báo cáo!");
    }
    SellerDashboardDTO dto = dashboardDAO.getSellerDashboardStats(sellerId);
    if (dto == null) {
      throw new ResourceNotFoundException("Không tìm thấy dữ liệu thống kê của người bán này.");
    }
    return dto;
  }
}