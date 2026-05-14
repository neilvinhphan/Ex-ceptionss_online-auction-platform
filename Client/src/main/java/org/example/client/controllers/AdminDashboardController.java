package org.example.client.controllers;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import org.example.client.network.ClientManager;
import org.example.core.dto.Request;
import org.example.core.dto.Response;

import java.util.Map;

public class AdminDashboardController {

    @FXML private Label lblTotalUsers, lblActiveAuctions, lblPendingItems, lblTotalVolume;

    @FXML private BarChart<String, Number> bcAuctionStatus;
    @FXML private PieChart pcCategoryDistribution;

    private Gson gson = ClientManager.getInstance().getGson();

    @FXML
    public void initialize() {
        loadDashboardData();
    }

    private void loadDashboardData() {
        new Thread(() -> {
            try {
                Request request = new Request("GET_ADMIN_DASHBOARD_STATS", null);
                String jsonRequest = gson.toJson(request);

                String jsonResponse = ClientManager.getInstance().getClient().sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);

                if ("SUCCESS".equals(response.getStatus())) {
                    JsonObject dataObj = gson.toJsonTree(response.getData()).getAsJsonObject();

                    Platform.runLater(() -> {
                        // 1. Cập nhật các thẻ KPI
                        updateKPIs(dataObj.getAsJsonObject("kpis"));

                        // 2. Cập nhật PieChart (Tỷ lệ ngành hàng theo Auction)
                        updatePieChart(dataObj.getAsJsonObject("categories"));

                        // 3. Cập nhật BarChart (Trạng thái phiên đấu giá - Thay cho TableView)
                        updateBarChart(dataObj.getAsJsonObject("auctionStatus"));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateKPIs(JsonObject kpis) {
        lblTotalUsers.setText(kpis.get("totalUsers").getAsString());
        lblActiveAuctions.setText(kpis.get("activeAuctions").getAsString());
        lblPendingItems.setText(kpis.get("pendingCount").getAsString());
        lblTotalVolume.setText(kpis.get("totalVolume").getAsString() + " đ");
    }

    private void updatePieChart(JsonObject categories) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, JsonElement> entry : categories.entrySet()) {
            // Hiển thị tên kèm số lượng trực tiếp trên label
            String label = entry.getKey() + " (" + entry.getValue().getAsInt() + ")";
            pieData.add(new PieChart.Data(label, entry.getValue().getAsInt()));
        }
        pcCategoryDistribution.setData(pieData);
    }

    private void updateBarChart(JsonObject statusData) {
        // Xóa dữ liệu cũ nếu có
        bcAuctionStatus.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Số lượng phiên");

        // Server trả về Map: {"Sắp diễn ra": 10, "Đang diễn ra": 5, ...}
        for (Map.Entry<String, JsonElement> entry : statusData.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue().getAsInt()));
        }

        bcAuctionStatus.getData().add(series);
    }
}