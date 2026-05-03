package org.example.client.controllers;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import org.example.client.network.ClientManager;
import org.example.client.utils.AuctionSession; // Đưa Trạm trung chuyển vào
import org.example.core.dto.Request;
import org.example.core.models.entities.Auction;
import org.example.core.models.items.Item;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionRoomController extends BaseController implements Initializable {

    @FXML private Label lblItemName, lblTimer, lblStatus, lblBid;
    @FXML private Label lblCurrentPrice, lblHighestBidder, lblWinner, lblBidError;
    @FXML private TextArea taDescription;
    @FXML private TextField tfBidAmount;
    @FXML private ListView<String> lvBidHistory;
    @FXML private LineChart<Number, Number> lineChart;
    @FXML private Button btnPlaceBid;

    private XYChart.Series<Number, Number> priceSeries; // để vẽ biểu đồ
    private ScheduledExecutorService timerService; // auto đếm ngược time

    private Auction currentAuction;
    private BigDecimal currentMaxPrice;
    private int bidStepCount = 0;
    private Gson gson;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Khởi tạo biểu đồ
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Biến động giá");
        lineChart.getData().add(priceSeries);

        // 2. Lấy Gson từ ClientManager
        gson = ClientManager.getInstance().getGson();

        // 3. LẤY DỮ LIỆU TỪ TRẠM TRUNG CHUYỂN
        Auction sessionAuction = AuctionSession.getInstance().getCurrentAuction();
        Item sessionItem = AuctionSession.getInstance().getCurrentItem();
// DÒNG THÁM TỬ ĐÂY:
        System.out.println("DEBUG: Auction có null ko? " + (sessionAuction == null));
        System.out.println("DEBUG: Item có null ko? " + (sessionItem == null));
        if (sessionAuction != null && sessionItem != null) {
            // Setup giao diện ngay lập tức
            setupRoom(sessionAuction, sessionItem);
        } else {
            showAlert("Lỗi", "Không tìm thấy dữ liệu phòng đấu giá!");
        }
    }

    private void setupRoom(Auction auction, Item item) {
        this.currentAuction = auction;
        this.currentMaxPrice = auction.getHighestBid() != null ? auction.getHighestBid() : item.getStartingPrice();

        lblItemName.setText(item.getItemName());
        taDescription.setText(item.getDescription());
        lblBid.setText(String.format("%,d VND", auction.getBidIncrement().longValue()));
        lblStatus.setText(auction.getStatus().toString());
        lblWinner.setText("--");
// 1. Mặc định ban đầu cứ cho là "Chưa có"
        String topBidder = "Chưa có";
        // 2. Kiểm tra xem lịch sử đặt giá đã có cái nào chưa?
        if (auction.getBidHistory() != null && !auction.getBidHistory().isEmpty()) {
            // Nếu có rồi, moi cái giao dịch cuối cùng ra
            int lastIndex = auction.getBidHistory().size() - 1;

            // Lấy tên của cái đứa nằm ở cuối danh sách
            // (LƯU Ý: Đệ nhớ tự đổi getBidderName() thành đúng cái tên hàm trong class BidTransaction của đệ nhé)
            topBidder = auction.getBidHistory().get(lastIndex).getBidderName();
        }

        // 3. Cập nhật lên Giao diện
        updatePriceUI(currentMaxPrice, "Chưa có");
        updateChart(currentMaxPrice);

        startCountdown(auction.getEndTime());
    }

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        String input = tfBidAmount.getText().trim();
        lblBidError.setText("");

        try {
            BigDecimal bidAmount = new BigDecimal(input);
            // Kiểm tra giá tại Client trước
            if (bidAmount.compareTo(currentMaxPrice) <= 0) {
                lblBidError.setText("Giá đặt phải cao hơn giá hiện tại!");
                return;
            }

            // TODO: Tạo Payload để gửi qua Socket (Nhớ dùng PlaceBidRequestDTO)

            tfBidAmount.clear();
            lblBidError.setStyle("-fx-text-fill: green;");
            lblBidError.setText("Đã gửi yêu cầu, đang chờ xác nhận...");

        } catch (NumberFormatException e) {
            lblBidError.setStyle("-fx-text-fill: red;");
            lblBidError.setText("Vui lòng nhập số tiền hợp lệ!");
        }
    }

    /**
     * HÀM NÀY ĐỂ SOCKET GỌI VÀO KHI NHẬN ĐƯỢC THÔNG BÁO TỪ SERVER
     * Lưu ý: Đã thêm tham số incomingAuctionId để check chống nhầm phòng
     */
    public void onNewBidBroadcastReceived(int incomingAuctionId, BigDecimal newPrice, String bidderName) {
        //  Chỉ cập nhật nếu tin nhắn thuộc về phòng đang xem
        if (this.currentAuction != null && incomingAuctionId == this.currentAuction.getAuctionId()) {
            Platform.runLater(() -> {
                this.currentMaxPrice = newPrice;
                updatePriceUI(newPrice, bidderName);
                updateChart(newPrice);

                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                lvBidHistory.getItems().add(0, String.format("[%s] %s đã đặt %,d VND", time, bidderName, newPrice.longValue()));

                lblBidError.setText(""); // Xóa thông báo chờ
            });
        }
    }

    /**
     * HÀM NÀY ĐỂ SOCKET GỌI VÀO KHI KẾT THÚC ĐẤU GIÁ
     */

    public void onAuctionEndBroadcastReceived(int incomingAuctionId, String winnerName, BigDecimal finalPrice) {
        // Chỉ kết thúc nếu đúng phòng đang xem
        if (this.currentAuction != null && incomingAuctionId == this.currentAuction.getAuctionId()) {
            Platform.runLater(() -> {
                stopTimer();
                lblTimer.setText("00:00:00");
                lblStatus.setText("ĐÃ KẾT THÚC"); // Đã sửa lblTimer1 thành lblStatus
                lblWinner.setText(winnerName);
                btnPlaceBid.setDisable(true);
                tfBidAmount.setDisable(true);

                showAlert("Thông báo", "Phiên đấu giá đã kết thúc!\nNgười chiến thắng: " + winnerName);
            });
        }
    }


    private void updatePriceUI(BigDecimal price, String bidder) {
        lblCurrentPrice.setText(String.format("%,d VND", price.longValue()));
        lblHighestBidder.setText(bidder);
    }

    private void updateChart(BigDecimal price) {
        bidStepCount++;
        priceSeries.getData().add(new XYChart.Data<>(bidStepCount, price.doubleValue()));
    }

    private void startCountdown(LocalDateTime endTime) {
        if (endTime == null) return;

        stopTimer();
        timerService = Executors.newSingleThreadScheduledExecutor();
        timerService.scheduleAtFixedRate(() -> {
            Duration duration = Duration.between(LocalDateTime.now(), endTime);
            Platform.runLater(() -> {
                if (duration.isNegative() || duration.isZero()) {
                    stopTimer();
                    lblTimer.setText("00:00:00");
                    lblStatus.setText("ĐÃ KẾT THÚC");
                    btnPlaceBid.setDisable(true);
                    tfBidAmount.setDisable(true);
                } else {
                    long h = duration.toHours();
                    long m = duration.toMinutesPart();
                    long s = duration.toSecondsPart();
                    lblTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
                }
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    @FXML
    private void handleBackToCatalog(ActionEvent event) {
        stopTimer();
        // Thoát phòng thì xóa Session đi cho sạch sẽ
        AuctionSession.getInstance().clearSession();
        switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục đấu giá");
    }

    @FXML
    private void handleMain(ActionEvent event) {
        stopTimer();
        AuctionSession.getInstance().clearSession();
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    private void stopTimer() {
        if (timerService != null && !timerService.isShutdown()) {
            timerService.shutdown();
        }
    }
}