package org.example.client.controllers;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.AuctionSession;
import org.example.client.utils.ImageUtils;
import org.example.client.utils.UserSession;
import org.example.core.dto.bidDTO.BidBroadcastDTO;
import org.example.core.dto.bidDTO.BidRequestDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.models.items.Item;
import org.example.core.models.users.User;

import java.io.BufferedReader;
import java.io.PrintWriter;
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

    // --- CÁC COMPONENT CŨ ---
    @FXML private Label lblItemName, lblTimer, lblStatus, lblBid;
    @FXML private Label lblCurrentPrice, lblHighestBidder, lblBidError;
    @FXML private TextArea taDescription;
    @FXML private TextField tfBidAmount;
    @FXML private ListView<String> lvBidHistory;
    @FXML private LineChart<Number, Number> lineChart;
    @FXML private Button btnPlaceBid;
    @FXML private ImageView ivItemImage;

    // --- CÁC COMPONENT MỚI THÊM CHO ROLE-BASED & UI MỚI ---
    @FXML private Label lblLiveViewersHeader;
    @FXML private StackPane spRolePanels;
    @FXML private VBox panelBidder, panelSeller, panelAdmin, vboxCheckoutSection;
    @FXML private CheckBox cbAutoBid;
    @FXML private Label lblSellerTotalBids, lblSellerProfit, lblPostAuctionStatus;
    @FXML private Button btnCheckout;
    @FXML private Label lblPriceTitle;
    @FXML private Label lblLeaderTitle;
    private XYChart.Series<Number, Number> priceSeries;
    private ScheduledExecutorService timerService;

    private Auction currentAuction;
    private BigDecimal currentMaxPrice;
    private int bidStepCount = 0;

    private Gson gson;
    private int currentAuctionId;
    private int currentUserId;
    private User currentUser;

    private PrintWriter outToServer;
    private BufferedReader inFromServer;
    private volatile boolean isListening = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            this.currentUserId = currentUser.getUserId();
        }

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Biến động giá");
        lineChart.getData().add(priceSeries);

        gson = ClientManager.getInstance().getGson();
        AuctionClient clientSocket = ClientManager.getInstance().getClient();

        outToServer = clientSocket.getOut();
        inFromServer = clientSocket.getIn();

        Auction sessionAuction = AuctionSession.getInstance().getCurrentAuction();
        Item sessionItem = AuctionSession.getInstance().getCurrentItem();

        if (sessionAuction != null && sessionItem != null) {
            setupRoom(sessionAuction, sessionItem);
            listenFromServer();
        } else {
            showAlert("Lỗi", "Không tìm thấy dữ liệu phòng đấu giá!");
        }

    }

    private void setupRoom(Auction auction, Item item) {
        this.currentAuction = auction;
        this.currentAuctionId = auction.getAuctionId();

        // Cấu hình UI theo Vai trò (Phân quyền Panel)
        setupRoleUI(currentUser, item);

        // 🛡️ PHÒNG THỦ: Tránh null khi chưa có lượt đặt giá nào
        this.currentMaxPrice =
                auction.getHighestBid() != null ? auction.getHighestBid() : (item != null ? item.getStartingPrice() : BigDecimal.ZERO);

        if (item != null) {
            lblItemName.setText(item.getItemName());
            taDescription.setText(item.getDescription());
            if (item.getImage() != null && !item.getImage().isEmpty()) {
                try {
                    Image decodedImage = ImageUtils.decodeBase64ToImage(item.getImage());
                    if (decodedImage != null) {
                        ivItemImage.setImage(decodedImage);
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi hiển thị ảnh trong phòng: " + e.getMessage());
                }
            }
        }

        if (auction.getBidIncrement() != null) {
            lblBid.setText(String.format("%,d VND", auction.getBidIncrement().longValue()));
        } else {
            lblBid.setText("Chưa có bước giá");
        }

        lblStatus.setText(auction.getStatus() != null ? auction.getStatus().toString() : "UNKNOWN");

        setupRolePanels(item);
        priceSeries.getData().clear();
        lvBidHistory.getItems().clear();
        bidStepCount = 0;

        if (auction.getBidHistory() != null && !auction.getBidHistory().isEmpty()) {
            for (BidTransaction bid : auction.getBidHistory()) {
                bidStepCount++;
                priceSeries.getData().add(new XYChart.Data<>(bidStepCount, bid.getAmount().doubleValue()));

                String time = bid.getTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                String historyLine =
                        String.format("[%s] %s đã đặt %,d VND", time, bid.getBidderName(), bid.getAmount().longValue());
                lvBidHistory.getItems().add(0, historyLine);
            }

            String topBidder = auction.getBidHistory().get(auction.getBidHistory().size() - 1).getBidderName();
            updatePriceUI(currentMaxPrice, topBidder);

            // Cập nhật số lượt đấu giá cho giao diện Seller
            if(lblSellerTotalBids != null) lblSellerTotalBids.setText(bidStepCount + " lượt");
        } else {
            if (item != null && item.getStartingPrice() != null) {
                priceSeries.getData().add(new XYChart.Data<>(0, item.getStartingPrice().doubleValue()));
            }
            updatePriceUI(currentMaxPrice, "Chưa có");
        }

        try {
            java.util.Map<String, Integer> joinData = new java.util.HashMap<>();
            joinData.put("auctionId", currentAuctionId);
            Request joinReq = new Request("JOIN_ROOM", joinData);
            outToServer.println(gson.toJson(joinReq));
        } catch (Exception e) {
            System.err.println("Lỗi gửi JOIN_ROOM: " + e.getMessage());
        }

        updateUiComponentsByStatus(auction.getStatus());
        startCountdown();
    }

    /**
     * HÀM MỚI: Phân quyền hiển thị StackPane dựa trên Role
     */
    private void setupRoleUI(User user, Item item) {
        // Mặc định ẩn tất cả
        panelBidder.setVisible(false); panelBidder.setManaged(false);
        panelSeller.setVisible(false); panelSeller.setManaged(false);
        panelAdmin.setVisible(false); panelAdmin.setManaged(false);

        if (user == null) {
            panelBidder.setVisible(true); panelBidder.setManaged(true);
            return;
        }

        // Logic check role (Cần chỉnh sửa getRole() / getSellerId() cho khớp với model của bạn)
        String role = String.valueOf(user.getRole()); // Giả sử getRole trả về String hoặc Enum

        if ("ADMIN".equalsIgnoreCase(role)) {
            panelAdmin.setVisible(true); panelAdmin.setManaged(true);
        }
        else if (item != null && user.getUserId() == item.getSellerID()) { // Sửa getOwnerId() thành getSellerId() nếu Model của bạn tên như vậy
            panelSeller.setVisible(true); panelSeller.setManaged(true);
        }
        else {
            panelBidder.setVisible(true); panelBidder.setManaged(true);
        }
    }

    // --- CÁC HÀM XỬ LÝ QUICK BID (THÊM MỚI) ---
    @FXML
    private void handleQuickBidStep(ActionEvent event) {
        if (currentAuction != null && currentAuction.getBidIncrement() != null) {
            BigDecimal nextBid = currentMaxPrice.add(currentAuction.getBidIncrement());
            tfBidAmount.setText(nextBid.toPlainString());
        }
    }

    @FXML
    private void handleQuickBid50k(ActionEvent event) {
        BigDecimal nextBid = currentMaxPrice.add(new BigDecimal("50000"));
        tfBidAmount.setText(nextBid.toPlainString());
    }

    @FXML
    private void handleQuickBid100k(ActionEvent event) {
        BigDecimal nextBid = currentMaxPrice.add(new BigDecimal("100000"));
        tfBidAmount.setText(nextBid.toPlainString());
    }

    // --- CÁC STUB METHOD CHO UI MỚI (TRÁNH LỖI LOAD FXML) ---
    @FXML private void handleReactHeart(ActionEvent event) { System.out.println("Thả tim!"); }
    @FXML private void handleReactFire(ActionEvent event) { System.out.println("Thả phẫn nộ/Lửa!"); }
    @FXML private void handleReactClap(ActionEvent event) { System.out.println("Vỗ tay!"); }
    @FXML private void handleEndAuctionEarly(ActionEvent event) { showAlert("Tính năng", "Chốt sớm đang phát triển!"); }
    @FXML private void handleCancelAuctionSeller(ActionEvent event) { showAlert("Tính năng", "Hủy phiên đang phát triển!"); }
    @FXML private void handlePauseAuction(ActionEvent event) { showAlert("Admin", "Tính năng Tạm dừng đang phát triển!"); }
    @FXML private void handleKickBidder(ActionEvent event) { showAlert("Admin", "Tính năng Kick đang phát triển!"); }


    // ======================================================================
    // LOGIC CŨ BÊN DƯỚI GIỮ NGUYÊN 100% ĐỂ KHÔNG ẢNH HƯỞNG SERVER
    // ======================================================================

    private void updateUiComponentsByStatus(org.example.core.shared.enums.AuctionStatus status) {
        if (status == org.example.core.shared.enums.AuctionStatus.OPEN) {
            tfBidAmount.setDisable(true);
            btnPlaceBid.setDisable(true);
            tfBidAmount.setPromptText("Phòng chưa mở cửa...");
        } else if (status == org.example.core.shared.enums.AuctionStatus.FINISHED) {
            tfBidAmount.setDisable(true);
            btnPlaceBid.setDisable(true);
            tfBidAmount.setPromptText("Phiên đã kết thúc.");
            if(btnCheckout != null) {
                btnCheckout.setVisible(true);
                btnCheckout.setManaged(true);
            }
        } else {
            tfBidAmount.setDisable(false);
            btnPlaceBid.setDisable(false);
            tfBidAmount.setPromptText("Nhập giá tiền...");
        }
    }

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        String input = tfBidAmount.getText().trim();
        lblBidError.setText("");

        try {
            BigDecimal bidAmount = new BigDecimal(input);
            if (bidAmount.compareTo(currentMaxPrice) <= 0) {
                lblBidError.setStyle("-fx-text-fill: red;");
                lblBidError.setText("Giá đặt phải cao hơn giá hiện tại!");
                return;
            }

            // TODO: Nếu checkbox AutoBid (cbAutoBid.isSelected()) được tích, bạn có thể gửi DTO khác lên Server sau này.

            BidRequestDTO bidReq = new BidRequestDTO(currentAuctionId, currentUserId, bidAmount);
            Request requestContainer = new Request("PLACE_BID", bidReq);

            if (outToServer != null) {
                outToServer.println(gson.toJson(requestContainer));
                tfBidAmount.clear();
                lblBidError.setStyle("-fx-text-fill: green;");
                lblBidError.setText("Đã gửi yêu cầu, đang chờ xác nhận...");
            } else {
                lblBidError.setStyle("-fx-text-fill: red;");
                lblBidError.setText("Lỗi mạng: Không thể gửi dữ liệu!");
            }
        } catch (NumberFormatException e) {
            lblBidError.setStyle("-fx-text-fill: red;");
            lblBidError.setText("Vui lòng nhập số tiền hợp lệ!");
        }
    }

    private void listenFromServer() {
        new Thread(
                () -> {
                    System.out.println("Đã kích hoạt luồng lắng nghe bộ đàm ngầm...");
                    try {
                        String messageFromServer;
                        while (isListening && (messageFromServer = inFromServer.readLine()) != null) {
                            if (!isListening) break;

                            try {
                                Response response = gson.fromJson(messageMessageFromServer(messageFromServer), Response.class);
                                if (response == null) continue;

                                if ("NEW_BID".equals(response.getStatus()) && response.getData() != null) {
                                    String innerData = gson.toJson(response.getData());
                                    BidBroadcastDTO data = gson.fromJson(innerData, BidBroadcastDTO.class);

                                    if (data != null) {
                                        int aId = data.getAuctionId();
                                        BigDecimal price = BigDecimal.valueOf(data.getNewPrice());
                                        String leader = data.getLeaderUsername();
                                        LocalDateTime endT = data.getNewEndTime();
                                        onNewBidBroadcastReceived(aId, price, leader, endT);
                                    }
                                }
                                else if ("ERROR_BID".equals(response.getStatus())) {
                                    Platform.runLater(() -> {
                                        lblBidError.setStyle("-fx-text-fill: red;");
                                        lblBidError.setText(response.getMessage());
                                    });
                                }
                                else if ("AUCTION_STARTED".equals(response.getStatus())) {
                                    Platform.runLater(() -> {
                                        if (currentAuction != null) {
                                            currentAuction.setStatus(org.example.core.shared.enums.AuctionStatus.RUNNING);
                                        }
                                        lblStatus.setText("RUNNING");
                                        updateUiComponentsByStatus(org.example.core.shared.enums.AuctionStatus.RUNNING);
                                        lblBidError.setStyle("-fx-text-fill: green;");
                                        lblBidError.setText(response.getMessage());
                                        startCountdown();
                                    });
                                }
                                else if ("AUCTION_ENDED".equals(response.getStatus())) {
                                    String winnerName = response.getMessage();
                                    Platform.runLater(() -> {
                                        stopTimer();
                                        lblTimer.setText("00:00:00");
                                        lblStatus.setText("FINISHED");
                                        btnPlaceBid.setDisable(true);
                                        tfBidAmount.setDisable(true);

                                        // ----- THÊM ĐOẠN ĐỔI CHỮ NÀY VÀO -----
                                        // Đổi tiêu đề thành Giá chốt và Người chiến thắng
                                        lblPriceTitle.setText("GIÁ CHỐT CUỐI CÙNG");
                                        lblLeaderTitle.setText("🏆 NGƯỜI CHIẾN THẮNG:");

                                        // Cập nhật tên người thắng và cho nó to lên, màu đỏ rực rỡ để nổi bật
                                        lblHighestBidder.setText(winnerName);
                                        lblHighestBidder.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 20px; -fx-font-weight: bold;");
                                        // ------------------------------------

                                        if (winnerName != null && winnerName.equals(UserSession.getInstance().getCurrentUser().getUserName())) {
                                            showAlert("Thông báo", "CHÚC MỪNG! BẠN ĐÃ TRỞ THÀNH CHỦ NHÂN CỦA MÓN ĐỒ!");
                                        } else {
                                            showAlert("Thông báo", "Phiên đấu giá đã kết thúc!\nNgười chiến thắng: " + winnerName);
                                        }
                                    });
                                }
                            } catch (Exception parseEx) {
                                System.err.println("❌ Lỗi bóc tách gói tin: " + parseEx.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        if (isListening) System.out.println("Ngắt kết nối luồng Socket ngầm: " + e.getMessage());
                    }
                })
                .start();
    }

    private String messageMessageFromServer(String raw) {
        return raw;
    }

    public void onNewBidBroadcastReceived(
            int incomingAuctionId, BigDecimal newPrice, String bidderName, LocalDateTime newEndTime) {

        if (this.currentAuction != null && incomingAuctionId == this.currentAuction.getAuctionId()) {
            Platform.runLater(
                    () -> {
                        this.currentMaxPrice = newPrice;
                        lblCurrentPrice.setText(String.format("%,d VND", newPrice.longValue()));
                        lblHighestBidder.setText(bidderName);

                        bidStepCount++;
                        priceSeries.getData().add(new XYChart.Data<>(bidStepCount, newPrice.doubleValue()));

                        // Cập nhật lượt đấu giá cho seller
                        if(lblSellerTotalBids != null) lblSellerTotalBids.setText(bidStepCount + " lượt");

                        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        String historyLine = String.format("[%s] %s đã đặt %,d VND", time, bidderName, newPrice.longValue());
                        lvBidHistory.getItems().add(0, historyLine);

                        lblBidError.setText("");

                        if (newEndTime != null) {
                            this.currentAuction.setEndTime(newEndTime);
                            startCountdown();
                        }
                    });
        }
    }

    private void updatePriceUI(BigDecimal price, String bidder) {
        lblCurrentPrice.setText(String.format("%,d VND", price.longValue()));
        lblHighestBidder.setText(bidder);
    }

    private void startCountdown() {
        if (currentAuction == null) return;
        stopTimer();

        timerService = Executors.newSingleThreadScheduledExecutor();
        timerService.scheduleAtFixedRate(
                () -> {
                    Platform.runLater(
                            () -> {
                                try {
                                    if (currentAuction == null) return;

                                    LocalDateTime now = LocalDateTime.now();
                                    LocalDateTime targetTime;

                                    if (currentAuction.getStatus() == org.example.core.shared.enums.AuctionStatus.OPEN) {
                                        targetTime = currentAuction.getStartTime();
                                    } else {
                                        targetTime = currentAuction.getEndTime();
                                    }

                                    if (targetTime == null) {
                                        lblTimer.setText("--:--:--");
                                        return;
                                    }

                                    Duration duration = Duration.between(now, targetTime);

                                    if (duration.isNegative() || duration.isZero()) {
                                        lblTimer.setText("00:00:00");
                                    } else {
                                        long h = duration.toHours();
                                        long m = duration.toMinutesPart();
                                        long s = duration.toSecondsPart();
                                        lblTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
                                    }
                                } catch (Exception e) {
                                    System.err.println("Cảnh báo nhẹ luồng đếm ngược: " + e.getMessage());
                                    lblTimer.setText("--:--:--");
                                }
                            });
                },
                0,
                1,
                TimeUnit.SECONDS);
    }

    @FXML
    private void handleBackToCatalog(ActionEvent event) {
        cleanUpBeforeExit();
        switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục đấu giá");
    }

    @FXML
    public void handleCheckout(ActionEvent event) {
        cleanUpBeforeExit();
        switchScene(event, "/views/WaitPaymentView.fxml", "Sản phẩm chờ thanh toán");
    }

    @FXML
    private void handleMain(ActionEvent event) {
        cleanUpBeforeExit();
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    private void cleanUpBeforeExit() {
        stopTimer();
        try {
            Request dummyReq = new Request("LEAVE_ROOM", null);
            if (outToServer != null) {
                outToServer.println(gson.toJson(dummyReq));
            }
        } catch (Exception e) {
            System.out.println("Lỗi gửi lệnh rời phòng: " + e.getMessage());
        }
        isListening = false;
        AuctionSession.getInstance().clearSession();
    }

    private void stopTimer() {
        if (timerService != null && !timerService.isShutdown()) {
            timerService.shutdown();
        }
    }
    private void setupRolePanels(Item currentItem) {
        // 1. Lấy thông tin user đang đăng nhập
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // 2. Giấu tất cả các bảng đi trước cho sạch sẽ
        panelBidder.setVisible(false); panelBidder.setManaged(false);
        panelSeller.setVisible(false); panelSeller.setManaged(false);
        panelAdmin.setVisible(false); panelAdmin.setManaged(false);

        // 3. Phân loại người dùng và bật đúng bảng
        String role = String.valueOf(currentUser.getRole()); // Tuỳ thuộc vào Entity User của bạn

        if ("ADMIN".equalsIgnoreCase(role)) {
            // Nếu là Admin -> Mở bảng Admin (có nút Hủy phòng, Kick user)
            panelAdmin.setVisible(true);
            panelAdmin.setManaged(true);

        } else if (currentItem != null && currentUser.getUserId() == currentItem.getSellerID()) {
            // Lưu ý: Sửa lại getSellerID() cho đúng với tên hàm trong class Item của bạn
            // Nếu ID của user hiện tại trùng với ID người bán món đồ -> Mở bảng Seller
            panelSeller.setVisible(true);
            panelSeller.setManaged(true);

        } else {
            // Còn lại tất cả những người khác -> Mở bảng Bidder (để đặt giá)
            panelBidder.setVisible(true);
            panelBidder.setManaged(true);
        }
    }
}