package org.example.client.controllers;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.userDTO.SellerDashboardDTO;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class RevenueController extends BaseController {

    @FXML private Label lblTotalRevenue;
    @FXML private Label lblTotalSold;
    @FXML private LineChart<String, Number> lineChartRevenue;
    @FXML private PieChart pieChartCategory;

    private final Gson gson = ClientManager.getInstance().getGson();
    private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

    @FXML
    public void initialize() {
        loadDashboardData();
    }

    private void loadDashboardData() {
        if (UserSession.getInstance().getCurrentUser() == null) return;
        int sellerId = UserSession.getInstance().getCurrentUser().getUserId();

        new Thread(() -> {
            try {
                Request request = new Request("GET_SELLER_DASHBOARD", sellerId);
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
                        });
                    } else {
                        Platform.runLater(() -> showAlert("Lỗi", response.getMessage()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Lỗi mạng", "Không thể tải dữ liệu: " + e.getMessage()));
            }
        }).start();
    }

    private void updateKPIs(double totalRevenue, int totalSold) {
        NumberFormat currencyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        lblTotalRevenue.setText(currencyFormatter.format(totalRevenue) + " đ");
        lblTotalSold.setText(String.valueOf(totalSold));
    }

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
}