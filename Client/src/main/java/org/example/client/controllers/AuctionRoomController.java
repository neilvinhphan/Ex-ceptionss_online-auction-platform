package org.example.client.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class AuctionRoomController extends BaseController implements Initializable {

    // Các thành phần UI từ FXML
    @FXML private Label lblTimer;
    @FXML private TextField tfBidAmount;
    @FXML private ListView<String> lvBidHistory;

    // Biến quản lý thời gian
    private Timeline countdownTimeline;
    private int secondsRemaining = 9015; // Giả sử lấy từ Server (02:30:15)

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startCountdown();
    }

    private void startCountdown() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (secondsRemaining > 0) {
                secondsRemaining--;
                updateTimerLabel();
            } else {
                stopAuction();
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateTimerLabel() {
        int h = secondsRemaining / 3600;
        int m = (secondsRemaining % 3600) / 60;
        int s = secondsRemaining % 60;
        lblTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    private void stopAuction() {
        if (countdownTimeline != null) countdownTimeline.stop();
        lblTimer.setText("ĐÃ KẾT THÚC");
        lblTimer.setStyle("-fx-text-fill: gray;");
        tfBidAmount.setDisable(true);
    }

    // Xử lý sự kiện nhấn nút "Đặt giá ngay"
    @FXML
    public void handlePlaceBid(ActionEvent event) {
        String bidText = tfBidAmount.getText().trim();

        try {
            double bidAmount = Double.parseDouble(bidText);
            // TODO: Gửi bidAmount lên Server qua WebSocket hoặc API ở đây

            // Tạm thời hiển thị vào danh sách lịch sử
            lvBidHistory.getItems().add("Bạn vừa đặt: " + bidAmount + " đ");
            tfBidAmount.clear();
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    // Xử lý sự kiện nhấn nút "Rời phòng"
    @FXML
    public void handleExitRoom(ActionEvent event) {
        if (countdownTimeline != null) countdownTimeline.stop();
        // Quay lại màn hình danh mục hoặc trang chủ
        switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục đấu giá");
    }
}