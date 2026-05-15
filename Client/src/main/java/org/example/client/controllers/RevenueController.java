package org.example.client.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import org.example.client.utils.UserSession;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class RevenueController extends BaseController {

    @FXML
    private Label lblTotalRevenue;
    @FXML
    private Label lblTotalSold;
    @FXML
    private LineChart<String, Number> lineChartRevenue;
    @FXML
    private PieChart pieChartCategory;

    @FXML
    public void initialize() {
        loadDashboardData();
    }

    private void loadDashboardData() {
        if (UserSession.getInstance().getCurrentUser() == null) return;

        int sellerId = UserSession.getInstance().getCurrentUser().getUserId();

        new Thread(() -> {
            try {
                //TODO : GỌI SOCKET Ở ĐÂY

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Lỗi", "Không thể tải dữ liệu: " + e.getMessage()));
            }
        }).start();
    }

    // --- CÁC HÀM UPDATE GIAO DIỆN (GIỮ NGUYÊN) ---

    private void updateKPIs(double totalRevenue, int totalSold) {
        NumberFormat currencyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        lblTotalRevenue.setText(currencyFormatter.format(totalRevenue) + " đ");
        lblTotalSold.setText(String.valueOf(totalSold));
    }

    private void updateLineChart(Map<String, Double> revenueData) {
        lineChartRevenue.getData().clear();
        if (revenueData == null || revenueData.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu");

        for (Map.Entry<String, Double> entry : revenueData.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        lineChartRevenue.getData().add(series);
    }

    private void updatePieChart(Map<String, Integer> categoryData) {

    }
}