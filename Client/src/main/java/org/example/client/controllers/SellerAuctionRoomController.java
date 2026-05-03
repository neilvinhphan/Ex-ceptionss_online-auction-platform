package org.example.client.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import org.example.core.models.items.Item;
import org.example.core.shared.enums.AuctionStatus;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SellerAuctionRoomController extends BaseController implements Initializable {

    @FXML private Label lblItemName, lblTimer, lblStatus, lblCurrentPrice, lblHighestBidder, lblWinner;
    @FXML private TextArea taDescription;
    @FXML private ListView<String> lvBidHistory;
    @FXML private LineChart<Number, Number> lineChart;
    @FXML private Button btnEndAuction, btnCancelAuction;

    private XYChart.Series<Number, Number> priceSeries;
    private ScheduledExecutorService timerService;
    private LocalDateTime auctionEndTime;
    private int bidCount = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Khởi tạo biểu đồ
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Diễn biến giá");
        lineChart.getData().add(priceSeries);
    }

    /**
     * HÀM QUAN TRỌNG: Nhận dữ liệu từ màn hình tạo đấu giá truyền sang
     */
    public void setAuctionData(Item item, long durationMinutes) {
        // 1. Hiển thị thông tin cơ bản
        lblItemName.setText(item.getItemName());
        taDescription.setText(item.getDescription());
        lblCurrentPrice.setText(item.getStartingPrice().toString() + " VND");
        lblStatus.setText("OPEN");

        // 2. Tính toán thời gian kết thúc dựa trên thời điểm hiện tại + duration
        this.auctionEndTime = LocalDateTime.now().plusMinutes(durationMinutes);

        // 3. Điểm đầu tiên trên biểu đồ là giá khởi điểm
        updateChart(item.getStartingPrice());

        // 4. Bắt đầu đếm ngược
        startCountdown();
    }

    private void startCountdown() {
        timerService = Executors.newSingleThreadScheduledExecutor();
        timerService.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(now, auctionEndTime);

            Platform.runLater(() -> {
                if (duration.isNegative() || duration.isZero()) {
                    handleAuctionEnd();
                } else {
                    long hours = duration.toHours();
                    long minutes = duration.toMinutesPart();
                    long seconds = duration.toSecondsPart();
                    lblTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                }
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void handleAuctionEnd() {
        lblTimer.setText("00:00:00");
        lblStatus.setText("CLOSED");
        btnEndAuction.setDisable(true);
        btnCancelAuction.setDisable(true);
        if (timerService != null) timerService.shutdown();
        showAlert("Thông báo", "Cuộc đấu giá đã kết thúc thời gian quy định!");
    }

    /**
     * Hàm này sẽ được gọi khi có thông điệp "NEW_BID" từ Server bắn về Client
     */
    public void updateNewBid(String bidderName, BigDecimal newPrice) {
        Platform.runLater(() -> {
            lblCurrentPrice.setText(newPrice.toString() + " VND");
            lblHighestBidder.setText(bidderName);
            lvBidHistory.getItems().add(0, "[" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + bidderName + " đặt: " + newPrice);
            updateChart(newPrice);
        });
    }

    private void updateChart(BigDecimal price) {
        bidCount++;
        priceSeries.getData().add(new XYChart.Data<>(bidCount, price.doubleValue()));
    }

    @FXML
    void handleEndEarly() {
        // TODO: Gửi request "END_AUCTION_EARLY" lên Server
        handleAuctionEnd();
    }

    @FXML
    void handleCancelAuction() {
        // TODO: Gửi request "CANCEL_AUCTION" lên Server
        if (timerService != null) timerService.shutdown();
        switchScene(null, "/views/WareHouseView.fxml", "Kho hàng");
    }

    @FXML
    void handleBackToWareHouse() {
        if (timerService != null) timerService.shutdown();
        // switchScene...
    }
}