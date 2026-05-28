package org.example.client.controllers.admin;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.example.client.controllers.BaseController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.admin.AdminDashboardDTO;
import org.example.core.shared.enums.ActionType;

/** Controller quản lý giao diện Dashboard của Admin. */
public class AdminDashboardController extends BaseController {

  private static final Logger logger = Logger.getLogger(AdminDashboardController.class.getName());

  @FXML private Label lblTotalUsers;
  @FXML private Label lblActiveAuctions;
  @FXML private Label lblPendingItems;
  @FXML private Label lblTotalVolume;
  @FXML private BarChart<String, Number> bcAuctionStatus;
  @FXML private PieChart pcCategoryDistribution;

  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  /** Khởi tạo controller, tự động chạy khi giao diện FXML được tải. */
  @FXML
  public void initialize() {
    loadDashboardData();
  }

  /**
   * Gửi yêu cầu lấy dữ liệu thống kê từ server thông qua một Thread riêng biệt và cập nhật lên giao
   * diện UI.
   */
  private void loadDashboardData() {
    if (UserSession.getInstance().getCurrentUser() == null) return;

    int adminId = UserSession.getInstance().getCurrentUser().getUserId();
    Request request = new Request(ActionType.GET_ADMIN_DASHBOARD_STATS, adminId);

    new Thread(
            () -> {
              try {
                String requestJson = gson.toJson(request);
                String jsonResponse = clientSocket.sendRequest(requestJson);

                if (jsonResponse != null) {
                  Response response = gson.fromJson(jsonResponse, Response.class);

                  Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                      String dataJson = gson.toJson(response.getData());
                      AdminDashboardDTO dashboardData = gson.fromJson(dataJson, AdminDashboardDTO.class);

                      updateKPIs(dashboardData.getKpis());
                      updatePieChart(dashboardData.getCategories());
                      updateBarChart(dashboardData.getAuctionStatus());
                    } else {
                      int code = response.getData() instanceof Number ? ((Number) response.getData()).intValue() : -1;
                      String errorTitle = switch (code) {
                        case 4030 -> "Quyền truy cập bị từ chối (403)";
                        case 5000 -> "Lỗi cơ sở dữ liệu hệ thống (500)";
                        default -> "Lỗi tải Dashboard (" + code + ")";
                      };
                      logger.warning("Lấy dữ liệu thống kê thất bại [" + code + "]: " + response.getMessage());
                      showAlert(errorTitle, response.getMessage());
                    }
                  });
                }
              } catch (Exception e) {
                logger.log(Level.SEVERE, "Lỗi nghiêm trọng xảy ra trong quá trình tải dữ liệu Dashboard", e);
                Platform.runLater(() -> showAlert("Lỗi kết nối", "Chi tiết lỗi: " + e.getMessage()));
              }
            })
            .start();
  }

  /**
   * Cập nhật các nhãn chỉ số KPI (Người dùng, Phiên đấu giá, Mục chờ duyệt, Doanh thu).
   *
   * @param kpis Map chứa các khóa và giá trị dạng chuỗi của KPI.
   */
  private void updateKPIs(Map<String, String> kpis) {
    logger.info("=== CẬP NHẬT CHỈ SỐ KPI ===");
    kpis.forEach((key, value) -> logger.info(key + " : " + value));

    lblTotalUsers.setText(kpis.getOrDefault("totalUsers", "0"));
    lblActiveAuctions.setText(kpis.getOrDefault("activeAuctions", "0"));
    lblPendingItems.setText(kpis.getOrDefault("pendingCount", "0"));
    String rawVolume = kpis.getOrDefault("totalVolume", "0");
    try {
      BigDecimal volumeValue = new BigDecimal(rawVolume);
      DecimalFormat formatter = new DecimalFormat("#,###", new DecimalFormatSymbols(new Locale("vi", "VN")));
      lblTotalVolume.setText(formatter.format(volumeValue) + " VNĐ");
    } catch (Exception e) {
      lblTotalVolume.setText(rawVolume + " VNĐ");
    }
  }

  /**
   * Cập nhật biểu đồ hình quạt biểu diễn phân bố danh mục sản phẩm.
   *
   * @param categories Map chứa tên danh mục và số lượng sản phẩm tương ứng.
   */
  private void updatePieChart(Map<String, Integer> categories) {
    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
    for (Map.Entry<String, Integer> entry : categories.entrySet()) {
      String label = entry.getKey() + " (" + entry.getValue() + ")";
      pieData.add(new PieChart.Data(label, entry.getValue()));
    }
    pcCategoryDistribution.setData(pieData);
  }

  /**
   * Cập nhật biểu đồ cột thể hiện số lượng phiên đấu giá theo từng trạng thái và đổi màu sắc các
   * cột tương ứng.
   *
   * @param statusData Map chứa tên trạng thái và số lượng phiên đấu giá.
   */
  private void updateBarChart(Map<String, Integer> statusData) {
    bcAuctionStatus.setAnimated(false);
    bcAuctionStatus.getData().clear();
    XYChart.Series<String, Number> series = new XYChart.Series<>();
    series.setName("Số lượng phiên");

    for (Map.Entry<String, Integer> entry : statusData.entrySet()) {
      series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
    }
    bcAuctionStatus.getData().add(series);

    for (XYChart.Data<String, Number> data : series.getData()) {
      Node bar = data.getNode();

      if (bar != null) {
        String status = data.getXValue();
        switch (status) {
          case "OPEN":
            bar.setStyle("-fx-bar-fill: #FFCCCC;");
            break;
          case "RUNNING":
            bar.setStyle("-fx-bar-fill: #2ecc71;");
            break;
          case "FINISHED":
            bar.setStyle("-fx-bar-fill: #e74c3c;");
            break;
          case "PAID":
            bar.setStyle("-fx-bar-fill: #CC66CC;");
            break;
          case "CANCELED":
            bar.setStyle("-fx-bar-fill: #777777;");
            break;
          default:
            bar.setStyle("-fx-bar-fill: #3498db;");
            break;
        }
      }
    }
  }
}
