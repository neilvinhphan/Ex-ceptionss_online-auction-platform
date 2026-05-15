package org.example.client.controllers;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.admin.AdminDashboardDTO; // Dùng luôn DTO từ Core

import java.util.Map;

public class AdminDashboardController extends BaseController {
    // Kế thừa BaseController để dùng tính năng chuyển trang/hiển thị thông báo (nếu cần)

    @FXML private Label lblTotalUsers, lblActiveAuctions, lblPendingItems, lblTotalVolume;
    @FXML private BarChart<String, Number> bcAuctionStatus;
    @FXML private PieChart pcCategoryDistribution;

    private final Gson gson = ClientManager.getInstance().getGson();
    private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

    @FXML
    public void initialize() {
        loadDashboardData();
    }

    private void loadDashboardData() {
        if (UserSession.getInstance().getCurrentUser() == null) return;

        // 1. Lấy ID Admin để gửi lên Server xác thực
        int adminId = UserSession.getInstance().getCurrentUser().getUserId();
        Request request = new Request("GET_ADMIN_DASHBOARD_STATS", adminId);

        new Thread(() -> {
            try {
                // 2. Gửi request qua Socket (Chuẩn y hệt các Controller khác của ông)
                clientSocket.getOut().println(gson.toJson(request));
                String jsonResponse = clientSocket.getIn().readLine();

                if (jsonResponse != null) {
                    Response response = gson.fromJson(jsonResponse, Response.class);

                    if ("SUCCESS".equals(response.getStatus())) {
                        // 3. Ép kiểu 1 phát ra luôn DTO, không cần JsonObject lằng nhằng
                        String dataJson = gson.toJson(response.getData());
                        AdminDashboardDTO dashboardData = gson.fromJson(dataJson, AdminDashboardDTO.class);

                        Platform.runLater(() -> {
                            updateKPIs(dashboardData.getKpis());
                            updatePieChart(dashboardData.getCategories());
                            updateBarChart(dashboardData.getAuctionStatus());
                        });
                    } else {
                        Platform.runLater(() -> showAlert("Lỗi", response.getMessage()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    private void updateKPIs(Map<String, String> kpis) {
        lblTotalUsers.setText(kpis.getOrDefault("totalUsers", "0"));
        lblActiveAuctions.setText(kpis.getOrDefault("activeAuctions", "0"));
        lblPendingItems.setText(kpis.getOrDefault("pendingCount", "0"));
        lblTotalVolume.setText(kpis.getOrDefault("totalVolume", "0"));
        // Lưu ý: chữ "đ" hoặc format tôi đã xử lý sẵn bên DAO của ông rồi nên cứ gắn thẳng vào là đẹp.
    }

    private void updatePieChart(Map<String, Integer> categories) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : categories.entrySet()) {
            String label = entry.getKey() + " (" + entry.getValue() + ")";
            pieData.add(new PieChart.Data(label, entry.getValue()));
        }
        pcCategoryDistribution.setData(pieData);
    }

    private void updateBarChart(Map<String, Integer> statusData) {
        bcAuctionStatus.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Số lượng phiên");

        for (Map.Entry<String, Integer> entry : statusData.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        bcAuctionStatus.getData().add(series);
        // 3. Lặp qua các cột để tô màu theo trạng thái
        for (XYChart.Data<String, Number> data : series.getData()) {
            javafx.scene.Node bar = data.getNode(); // Lấy cục UI của cái cột

            if (bar != null) {
                String status = data.getXValue();
                switch (status) {
                    case "OPEN":
                        bar.setStyle("-fx-bar-fill: #CC66CC;");
                        break;
                    case "RUNNING":
                        bar.setStyle("-fx-bar-fill: #2ecc71;");
                        break;
                    case "Sắp diễn ra":
                        bar.setStyle("-fx-bar-fill: #f39c12;"); // Cam
                        break;
                    case "Đã kết thúc":
                        bar.setStyle("-fx-bar-fill: #e74c3c;"); // Đỏ
                        break;
                    case "Đã hủy":
                        bar.setStyle("-fx-bar-fill: #95a5a6;"); // Xám
                        break;
                    default:
                        bar.setStyle("-fx-bar-fill: #3498db;"); // Xanh dương (mặc định)
                        break;
                }
            }
        }
    }
}