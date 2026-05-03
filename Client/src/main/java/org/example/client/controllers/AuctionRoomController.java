package org.example.client.controllers;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.Duration;
import java.net.URL;
import java.util.ResourceBundle;
public class AuctionRoomController extends BaseController implements Initializable {
    @FXML private Label lblItemName;
    @FXML private Label lblTimer;
    @FXML private Label lblStatus;
    @FXML private TextArea taDescription;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblHighestBidder;
    @FXML private Label lblWinner;
    @FXML private TextField tfBidAmount;
    @FXML private Button btnBid;
    @FXML private Label lblError;
    @FXML private ListView<String> lvBidHistory;
    @FXML private LineChart<Number, Number> lineChart;
    // ===== TIMER =====
    private Timeline countdownTimeline;
    private long endTime;
    // ===== CHART =====
    private XYChart.Series<Number, Number> priceSeries = new XYChart.Series<>();
    private int bidIndex = 0;

    // ===== CALLBACK (inject từ ngoài) =====
    private BidHandler bidHandler;

    @FXML
    public void handleMain(ActionEvent event) {
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    @FXML
    public void handleExit(ActionEvent event) {
        switchScene(event, "/views/AuctionCatalogView.fxml", "Danh sách phòng đấu giá");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (lineChart == null) return; // tránh crash ui
        // setup chart
        lineChart.getData().add(priceSeries);
        lineChart.setCreateSymbols(false);
        // xử lý nhập bidAmount: vd 123->123, abc-> " "
        tfBidAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                tfBidAmount.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
        // UI state ban đầu
        btnBid.setDisable(true); // chưa có data thì chưa cho bid
    }

    // ================= NHẬN DATA TỪ SERVER =================
    // lấy dữ liệu từ server → đổ vào giao diện
    public void renderAuction(String itemName, double price, String bidder, String status, long endTimeFromServer) {
        lblItemName.setText(itemName);
        lblCurrentPrice.setText(formatPrice(price));
        lblHighestBidder.setText(bidder);
        lblStatus.setText(status);
        this.endTime = endTimeFromServer;
        btnBid.setDisable(false);
        startCountdown();
    }

    // ================= UPDATE KHI CÓ NGƯỜI BID =================
    // 👉 server push về thì gọi cái này
    public void onNewBid(double price, String bidder) {
        bidIndex++; //mỗi lần có bid mới → tăng trục X
        lvBidHistory.getItems().add(bidder + ": " + formatPrice(price)); //Thêm vào lịch sử
        priceSeries.getData().add(new XYChart.Data<>(bidIndex, price)); //Vẽ lên biểu đồ
        lblCurrentPrice.setText(formatPrice(price));
        lblHighestBidder.setText(bidder);
        highlightPrice();
    }

    // ================= TIMER =================
    private void startCountdown() {
        if (countdownTimeline != null) { // nếu có timer cũ -> dừng nó trước -> tạo timer mới
            countdownTimeline.stop();
        }
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> { //cứ 1 giây chạy đoạn code bên trong
            long now = System.currentTimeMillis();
            long secondsRemaining = (endTime - now) / 1000;
            if (secondsRemaining > 0) {
                updateTimerLabel((int) secondsRemaining);
            } else {
                stopAuction();
            }

        }));

        countdownTimeline.setCycleCount(Timeline.INDEFINITE); // chạy mãi
        countdownTimeline.play(); // kích hoạt timer
    }

    private void updateTimerLabel(int secondsRemaining) { // hàm hiển thị tgian còn lại
        int h = secondsRemaining / 3600;
        int m = (secondsRemaining % 3600) / 60;
        int s = secondsRemaining % 60;
        lblTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    private void stopAuction() {
        if (countdownTimeline != null) countdownTimeline.stop();
        lblTimer.setText("ĐÃ KẾT THÚC");
        lblTimer.setStyle("-fx-text-fill: gray;");
        btnBid.setDisable(true);
        tfBidAmount.setDisable(true);
        lblStatus.setText("FINISHED");
    }

    // ================= USER ACTION =================
    @FXML
    public void handlePlaceBid() { //lấy giá user nhập → kiểm tra → gửi ra ngoài → chờ server phản hồi
        String bidText = tfBidAmount.getText().trim();
        if (bidText.isEmpty()) {
            lblError.setText("Không được để trống!");
            return;
        }
        try {
            double bidAmount = Double.parseDouble(bidText);
            // disable để tránh spam
            btnBid.setDisable(true);
            lblStatus.setText("Đang gửi giá...");

            // 👉 gọi ra ngoài (UI KHÔNG biết server làm gì)
            if (bidHandler != null) {
                bidHandler.onBid(bidAmount); // thực chất là socket.senBid()
            }

            tfBidAmount.clear();
            lblError.setText(""); //dọn UI sau khi gửi

        } catch (NumberFormatException e) {
            lblError.setText("Giá không hợp lệ!");
        }
    }

    // ================= CALLBACK SETTER =================
    public void setBidHandler(BidHandler handler) {
        this.bidHandler = handler;
    }

    // server phản hồi (success/fail)
    public void onBidResult(boolean success) {
        btnBid.setDisable(false);
        lblStatus.setText(success ? "Đang đấu giá" : "Bid thất bại");
    }

    private void highlightPrice() {
        lblCurrentPrice.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold; -fx-font-size: 28;");
    }

    private String formatPrice(double price) {
        return String.format("%,.0f đ", price);
    }

    // ================= INTERFACE =================
    public interface BidHandler {
        void onBid(double amount);
    }
}