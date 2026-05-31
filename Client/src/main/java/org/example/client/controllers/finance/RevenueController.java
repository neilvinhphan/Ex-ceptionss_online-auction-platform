package org.example.client.controllers.finance;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import org.example.client.controllers.BaseController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.userDTO.SellerDashboardDTO;
import org.example.core.shared.enums.ActionType;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller chịu trách nhiệm quản lý màn hình báo cáo doanh thu của người bán (Seller Dashboard).
 * Thực hiện truy vấn dữ liệu tài chính, thống kê KPI, biểu đồ tròn phân loại và biểu đồ đường doanh
 * thu tích lũy.
 */
public class RevenueController extends BaseController {

  private static final Logger logger = Logger.getLogger(RevenueController.class.getName());

  @FXML private Label lblTotalRevenue;
  @FXML private Label lblTotalSold;
  @FXML private LineChart<String, Number> lineChartRevenue;
  @FXML private PieChart pieChartCategory;

  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  /** Phương thức khởi tạo giao diện, kích hoạt tiến trình tải dữ liệu từ máy chủ. */
  @FXML
  public void initialize() {
    loadDashboardData();
  }

  /**
   * Luồng ngầm gửi yêu cầu Socket lấy toàn bộ thông tin thống kê tài chính của người bán hiện tại.
   */
  private void loadDashboardData() {
    if (UserSession.getInstance().getCurrentUser() == null) return;
    int sellerId = UserSession.getInstance().getCurrentUser().getUserId();

    new Thread(() -> {
      try {
        Request request = new Request(ActionType.GET_SELLER_DASHBOARD, sellerId);
        clientSocket.getOut().println(gson.toJson(request));

        String jsonResponse = clientSocket.getIn().readLine();
        if (jsonResponse != null) {
          Response response = gson.fromJson(jsonResponse, Response.class);

          if ("SUCCESS".equals(response.getStatus())) {
            String dataJson = gson.toJson(response.getData());
            SellerDashboardDTO dto = gson.fromJson(dataJson, SellerDashboardDTO.class);

            Platform.runLater(() -> {
              updateKPIs(dto.getTotalRevenue(), dto.getTotalSold());
              updatePieChart(dto.getCategoryData());
              updateLineChart(dto.getRevenueGrowthData());
            });
          } else {
            Platform.runLater(() -> {
              int code = response.getData() instanceof Number ? ((Number) response.getData()).intValue() : -1;
              String title = (code == 4030) ? "Không có quyền Seller (403)" : "Lỗi Server (" + code + ")";
              showAlert(title, response.getMessage());
            });
          }
        }
      } catch (Exception e) {
        Platform.runLater(() -> showAlert("Lỗi mạng", "Không thể tải dữ liệu: " + e.getMessage()));
      }
    }).start();
  }

  /** Cập nhật các chỉ số tài chính cơ bản (KPI Labels) kèm định dạng tiền tệ VNĐ chuẩn. */
  private void updateKPIs(double totalRevenue, int totalSold) {
    NumberFormat currencyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));
    lblTotalRevenue.setText(currencyFormatter.format(totalRevenue) + " VNĐ");
    lblTotalSold.setText(String.valueOf(totalSold));
  }

  /** Đồng bộ hóa và kết xuất dữ liệu cơ cấu ngành hàng lên biểu đồ hình tròn (PieChart). */
  private void updatePieChart(Map<String, Integer> categoryData) {
    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
    if (categoryData != null) {
      for (Map.Entry<String, Integer> entry : categoryData.entrySet()) {
        String label = entry.getKey() + " (" + entry.getValue() + ")";
        pieData.add(new PieChart.Data(label, entry.getValue()));
      }
    }
    pieChartCategory.setData(pieData);
  }

  /** Làm sạch dữ liệu cũ và cập nhật đường biểu diễn tăng trưởng doanh thu tích lũy lên LineChart. */
  private void updateLineChart(Map<String, Double> revenueGrowthData) {
    lineChartRevenue.getData().clear();

    if (revenueGrowthData == null || revenueGrowthData.isEmpty()) {
      return;
    }

    XYChart.Series<String, Number> series = new XYChart.Series<>();
    series.setName("Doanh thu tích lũy");

    for (Map.Entry<String, Double> entry : revenueGrowthData.entrySet()) {
      String orderLabel = entry.getKey();
      Double revenueValue = entry.getValue();
      series.getData().add(new XYChart.Data<>(orderLabel, revenueValue));
    }

    lineChartRevenue.getData().add(series);
  }
}
