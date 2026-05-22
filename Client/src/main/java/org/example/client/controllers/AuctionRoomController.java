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
import javafx.scene.layout.VBox;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.AuctionSession;
import org.example.client.utils.ImageUtils;
import org.example.client.utils.UserSession;
import org.example.core.dto.bidDTO.BidBroadcastDTO;
import org.example.core.dto.bidDTO.BidRequestDTO;
import org.example.core.dto.bidDTO.AutoBidRequestDTO; // 🔥 KHỚP NỐI: Import DTO nhận thầu mới từ Core
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

    @FXML private Label lblItemName, lblTimer, lblStatus, lblBid;
    @FXML private Label lblCurrentPrice, lblHighestBidder, lblWinner, lblBidError;
    @FXML private Label lblOnlineCount; // Chèn cạnh các nhãn Label cũ của bro
    @FXML private TextArea taDescription;
    @FXML private TextField tfBidAmount;
    @FXML private ListView<String> lvBidHistory;
    @FXML private LineChart<Number, Number> lineChart;
    @FXML private Button btnPlaceBid;
    @FXML private ImageView ivItemImage;
    @FXML private VBox vboxBidderControls;
    @FXML private VBox vboxSellerControls;
    @FXML private VBox vboxAdminControls;
    // --- THÊM LINH KIỆN ĐỒ HỌA AUTOBID ---
    @FXML private TextField tfMaxBid;
    @FXML private Button btnToggleAutoBid;
    @FXML private VBox vboxPriceBox;
    @FXML private VBox vboxWinnerBox;
    private XYChart.Series<Number, Number> priceSeries;
    private ScheduledExecutorService timerService;

    private Auction currentAuction;
    private BigDecimal currentMaxPrice;
    private int bidStepCount = 0;
    private boolean isAutoBidActive = false; // Biến cờ theo dõi trạng thái Bật/Tắt Bot của bản thân
    private int bidderId;

    private Gson gson;
    private int currentAuctionId;
    private int currentUserId;

    private PrintWriter outToServer;
    private BufferedReader inFromServer;
    private volatile boolean isListening = true;

    // 🔥 HÀM THÊM MỚI: Xử lý ẩn/hiện giao diện theo danh tính
    private void setupRoleBasedUI(User currentUser, Auction auction) {
        // 1. Reset: Ẩn và giải phóng không gian của cả 3 bảng điều khiển
        vboxBidderControls.setVisible(false); vboxBidderControls.setManaged(false);
        vboxSellerControls.setVisible(false); vboxSellerControls.setManaged(false);
        vboxAdminControls.setVisible(false); vboxAdminControls.setManaged(false);

        // 2. Lấy thông tin user hiện tại
        if (currentUser == null) return;
        String role = currentUser.getRole().toString();
        int userId = currentUser.getUserId();
        System.out.println("DEBUG - ID của tôi là: " + userId);
        System.out.println("DEBUG - ID của Chủ phòng là: " + auction.getOwnerId());
        // 3. Phân luồng hiển thị
        if (role != null && role.equalsIgnoreCase("ADMIN")) {
            // LÀ ADMIN: Mở khu vực quyền lực
            vboxAdminControls.setVisible(true);
            vboxAdminControls.setManaged(true);

        } else if ( userId == auction.getOwnerId()) {
            // LÀ SELLER: Mở bảng theo dõi trạng thái
            vboxSellerControls.setVisible(true);
            vboxSellerControls.setManaged(true);

        } else {
            // CÒN LẠI: Mở khu vực nhập giá cho dân chơi (Bidder)
            vboxBidderControls.setVisible(true);
            vboxBidderControls.setManaged(true);
        }
    }
    private void showWinnerBox(String winnerName) {
        // 1. Ẩn ô màu xanh lá (Giá hiện tại)
        vboxPriceBox.setVisible(false);
        vboxPriceBox.setManaged(false);

        // 2. Hiện ô màu vàng (Người chiến thắng)
        vboxWinnerBox.setVisible(true);
        vboxWinnerBox.setManaged(true);

        // 3. Ghi tên người thắng vào (Nếu không có ai mua thì ghi "Không có")
        if (winnerName != null && !winnerName.isEmpty()) {
            lblWinner.setText(winnerName);
        } else {
            lblWinner.setText("Không có ai");
        }
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (UserSession.getInstance().getCurrentUser() != null) {
            this.currentUserId = UserSession.getInstance().getCurrentUser().getUserId();
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
        User user = UserSession.getInstance().getCurrentUser();
        setupRoleBasedUI(user, auction);
        this.currentMaxPrice =
                auction.getHighestBid() != null ? auction.getHighestBid() : (item != null ? item.getStartingPrice() : BigDecimal.ZERO);

//        if (item != null) {
//            lblItemName.setText(item.getItemName());
//            taDescription.setText(item.getDescription());
//            if (item.getImage() != null && !item.getImage().isEmpty()) {
//                try {
//                    Image decodedImage = ImageUtils.decodeBase64ToImage(item.getImage());
//                    if (decodedImage != null) {
//                        ivItemImage.setImage(decodedImage);
//                    }
//                } catch (Exception e) {
//                    System.err.println("Lỗi hiển thị ảnh trong phòng: " + e.getMessage());
//                }
//            }
//        }

// Ưu tiên lấy Item từ trong Auction, nếu không có mới dùng Item truyền vào
        Item actualItem = (auction.getItem() != null) ? auction.getItem() : item;

        if (actualItem != null) {
            lblItemName.setText(actualItem.getItemName());
            if (actualItem.getDescription() != null && !actualItem.getDescription().isEmpty()) {
                taDescription.setText(actualItem.getDescription());
            } else {
                taDescription.setText("Chưa có mô tả chi tiết cho sản phẩm này.");
            }
            if (actualItem.getImage() != null && !actualItem.getImage().isEmpty()) {
                try {
                    Image decodedImage = ImageUtils.decodeBase64ToImage(actualItem.getImage());
                    if (decodedImage != null) {
                        ivItemImage.setImage(decodedImage);
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi hiển thị ảnh trong phòng: " + e.getMessage());
                }
            }
        } else {
            // Dự phòng trường hợp xấu nhất: Cả hai đều null
            lblItemName.setText(auction.getItemName() != null ? auction.getItemName() : "Sản phẩm không xác định");
            taDescription.setText("Dữ liệu sản phẩm đang bị lỗi. Vui lòng liên hệ Admin.");
        }
        if (auction.getBidIncrement() != null) {
            lblBid.setText(String.format("%,d VND", auction.getBidIncrement().longValue()));
        } else {
            lblBid.setText("Chưa có bước giá");
        }

        lblStatus.setText(auction.getStatus() != null ? auction.getStatus().toString() : "UNKNOWN");
        lblWinner.setText("--");

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

    private void updateUiComponentsByStatus(org.example.core.shared.enums.AuctionStatus status) {
        boolean disable = (status == org.example.core.shared.enums.AuctionStatus.OPEN || status == org.example.core.shared.enums.AuctionStatus.FINISHED);
        tfBidAmount.setDisable(disable);
        btnPlaceBid.setDisable(disable);
        tfMaxBid.setDisable(disable || isAutoBidActive); // Khóa ô nhập trần nếu chưa mở cửa hoặc Bot đang chạy
        btnToggleAutoBid.setDisable(disable);

        if (status == org.example.core.shared.enums.AuctionStatus.OPEN) {
            tfBidAmount.setPromptText("Phòng chưa mở cửa...");
            tfMaxBid.setPromptText("Phòng chưa mở cửa...");
        } else if (status == org.example.core.shared.enums.AuctionStatus.FINISHED) {
            tfBidAmount.setPromptText("Phiên đã kết thúc.");
            tfMaxBid.setPromptText("Phiên đã kết thúc.");
        } else {
            if (!isAutoBidActive) tfMaxBid.setPromptText("Nhập hạn mức trần...");
            tfBidAmount.setPromptText("Nhập giá tiền...");
        }
    }

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        String input = tfBidAmount.getText().trim();
        lblBidError.setText("");

        String myUsername = UserSession.getInstance().getCurrentUser().getUserName();
        String leadingUsername = lblHighestBidder.getText().trim();

        try {
            BigDecimal bidAmount = new BigDecimal(input);

            if (myUsername.equalsIgnoreCase(leadingUsername)) {
                lblBidError.setStyle("-fx-text-fill: red;");
                lblBidError.setText("Bạn đang là người dẫn đầu phòng!");
                return;
            }

            if (bidAmount.compareTo(currentMaxPrice) <= 0) {
                lblBidError.setStyle("-fx-text-fill: red;");
                lblBidError.setText("Giá đặt phải cao hơn giá hiện tại!");
                return;
            }

            BidRequestDTO bidReq = new BidRequestDTO(currentAuctionId, currentUserId, bidAmount);
            Request requestContainer = new Request("PLACE_BID", bidReq);

            if (outToServer != null) {
                outToServer.println(gson.toJson(requestContainer));
                tfBidAmount.clear();
                lblBidError.setStyle("-fx-text-fill: green;");
                lblBidError.setText("Đã gửi yêu cầu, đang chờ xác nhận...");
            }
        } catch (NumberFormatException e) {
            lblBidError.setStyle("-fx-text-fill: red;");
            lblBidError.setText("Vui lòng nhập số tiền hợp lệ!");
        }
    }

    // 🔥 HÀM THÊM MỚI: Xử lý cơ chế Lật trạng thái Bật/Tắt Bot AutoBid cực kỳ mượt mà
    @FXML
    private void handleToggleAutoBid(ActionEvent event) {
        lblBidError.setText("");
        if (!isAutoBidActive) {
            // LUỒNG BẬT BOT
            String input = tfMaxBid.getText().trim();
            if (input.isEmpty()) {
                lblBidError.setStyle("-fx-text-fill: red;");
                lblBidError.setText("Vui lòng nhập số tiền trần tối đa!");
                return;
            }
            try {
                BigDecimal maxBid = new BigDecimal(input);
                if (maxBid.compareTo(currentMaxPrice) <= 0) {
                    lblBidError.setStyle("-fx-text-fill: red;");
                    lblBidError.setText("Giá trần của Bot phải lớn hơn giá hiện tại!");
                    return;
                }

                // Đóng gói chuyển dữ liệu lên Server kích hoạt mìn
                AutoBidRequestDTO autoBidReq = new AutoBidRequestDTO(currentAuctionId, currentUserId, maxBid);
                outToServer.println(gson.toJson(new Request("REGISTER_AUTOBID", autoBidReq)));

                // Ép đổi giao diện sang màu Đỏ cảnh báo hủy diệt
                isAutoBidActive = true;
                tfMaxBid.setDisable(true);
                btnToggleAutoBid.setText("HỦY AUTOBID");
                btnToggleAutoBid.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;");
                lblBidError.setStyle("-fx-text-fill: green;");
                lblBidError.setText("Hệ thống AutoBid đã được kích hoạt thành công!");

            } catch (NumberFormatException e) {
                lblBidError.setStyle("-fx-text-fill: red;");
                lblBidError.setText("Số tiền cài trần không hợp lệ!");
            }
        } else {
            // LUỒNG TẮT BOT GIỮA CHỪNG
            AutoBidRequestDTO cancelReq = new AutoBidRequestDTO(currentAuctionId, currentUserId, BigDecimal.ZERO);
            outToServer.println(gson.toJson(new Request("CANCEL_AUTOBID", cancelReq)));

            // Trả nút bấm về màu Cam rực rỡ ban đầu
            isAutoBidActive = false;
            tfMaxBid.setDisable(false);
            tfMaxBid.clear();
            tfMaxBid.setPromptText("Nhập hạn mức trần...");
            btnToggleAutoBid.setText("KÍCH HOẠT AUTOBID");
            btnToggleAutoBid.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold;");
            lblBidError.setStyle("-fx-text-fill: green;");
            lblBidError.setText("Hệ thống AutoBid đã ngừng hoạt động!");
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
                                Response response = gson.fromJson(messageFromServer, Response.class);
                                if (response == null) continue;

                                if ("NEW_BID".equals(response.getStatus()) && response.getData() != null) {
                                    String innerData = gson.toJson(response.getData());
                                    BidBroadcastDTO data = gson.fromJson(innerData, BidBroadcastDTO.class);

                                    if (data != null) {
                                        int aId = data.getAuctionId();
                                        BigDecimal price = BigDecimal.valueOf(data.getNewPrice());
                                        String leader = data.getLeaderUsername();
                                        LocalDateTime endT = data.getNewEndTime();
                                        boolean isAutoTriggered = data.isAutoBidTriggered(); // 🔥 KHỚP NỐI CORE: Đón cờ AutoBid

                                        // Ném thêm tham số cờ truyền xuống UI render đồ họa
                                        onNewBidBroadcastReceived(aId, price, leader, endT, isAutoTriggered);
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
//                                else if ("AUCTION_END".equals(response.getStatus())) {
//                                    String winnerName = response.getMessage();
//                                    Platform.runLater(() -> {
//                                        stopTimer();
//                                        lblTimer.setText("00:00:00");
//                                        lblStatus.setText("FINISHED");
//                                        showWinnerBox(winnerName);
//                                        lblWinner.setText(winnerName != null ? winnerName : "Không có");
//                                        updateUiComponentsByStatus(org.example.core.shared.enums.AuctionStatus.FINISHED);
//
//                                        User user = UserSession.getInstance().getCurrentUser();
//                                        if (winnerName != null && user != null && winnerName.equals(user.getUserName())) {
//                                            showAlert("Thông báo", "CHÚC MỪNG! BẠN ĐÃ TRỞ THÀNH CHỦ NHÂN CỦA MÓN ĐỒ!");
//                                        } else {
//                                            showAlert("Thông báo", "Phiên đấu giá đã kết thúc!\nNgười chiến thắng: " + winnerName);
//                                        }
//                                    });
//                                }
                                else if ("AUCTION_END".equals(response.getStatus())) {
                                    // 1. Lấy dữ liệu đính kèm (nếu có) và loại bỏ dấu ngoặc kép thừa do Gson sinh ra
                                    String additionalData = "";
                                    if (response.getData() != null) {
                                        additionalData = response.getData().toString().replace("\"", "");
                                    }

                                    // 2. Rẽ nhánh: Nếu là Admin hủy thì đếm ngược, nếu không thì báo người thắng
                                    if ("ADMIN_CANCELLED".equals(additionalData)) {

                                        // LUỒNG 1: BỊ ADMIN HỦY KHẨN CẤP
                                        Platform.runLater(() -> {
                                            showCancelAlertAndCountdown(); // Gọi hàm đếm ngược 15s ở cuối file
                                        });

                                    } else {

                                        // LUỒNG 2: KẾT THÚC BÌNH THƯỜNG (CÓ NGƯỜI THẮNG)
                                        String winnerName = response.getMessage();

                                        Platform.runLater(() -> {
                                            stopTimer();
                                            lblTimer.setText("00:00:00");
                                            lblStatus.setText("FINISHED");
                                            showWinnerBox(winnerName);
                                            lblWinner.setText(winnerName != null ? winnerName : "Không có");
                                            updateUiComponentsByStatus(org.example.core.shared.enums.AuctionStatus.FINISHED);

                                            User user = UserSession.getInstance().getCurrentUser();
                                            if (winnerName != null && user != null && winnerName.equals(user.getUserName())) {
                                                showAlert("Thông báo", "CHÚC MỪNG! BẠN ĐÃ TRỞ THÀNH CHỦ NHÂN CỦA MÓN ĐỒ!");
                                            } else {
                                                showAlert("Thông báo", "Phiên đấu giá đã kết thúc!\nNgười chiến thắng: " + winnerName);
                                            }
                                        });
                                    }
                                }
                                else if ("MY_AUTOBID_STATUS".equals(response.getStatus())) {
                                    try {
                                        String innerData = gson.toJson(response.getData());
                                        // Bóc lấy số tiền trần cũ từ Server gửi về
                                        java.util.Map<String, Double> dataMap = gson.fromJson(innerData, java.util.Map.class);
                                        double savedMaxBid = dataMap.get("maxBid");

                                        Platform.runLater(() -> {
                                            // 🔥 KHÔI PHỤC NGUYÊN VẸN GIAO DIỆN BOT
                                            isAutoBidActive = true;
                                            tfMaxBid.setText(String.format("%.0f", savedMaxBid)); // Điền lại số tiền cũ
                                            tfMaxBid.setDisable(true); // Khóa ô nhập thầu

                                            // Ép nút bấm sang trạng thái HỦY màu Đỏ
                                            btnToggleAutoBid.setText("HỦY AUTOBID");
                                            btnToggleAutoBid.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;");

                                            lblBidError.setStyle("-fx-text-fill: green;");
                                            lblBidError.setText("Hệ thống nhận diện: Bot AutoBid của bạn đang gác phòng này!");
                                        });
                                    } catch (Exception e) {
                                        System.err.println("Lỗi parse trạng thái AutoBid cũ: " + e.getMessage());
                                    }
                                }
                                else if ("ROOM_USER_COUNT".equals(response.getStatus())) {
                                    String countStr = response.getMessage(); // Server sẽ ném số lượng người vào trường Message
                                    Platform.runLater(() -> {
                                        lblOnlineCount.setText("| 👥 Đang xem: " + countStr + " người");
                                    });
                                }
                            } catch (Exception parseEx) {
                                System.err.println("❌ Lỗi bóc tách gói tin (Bỏ qua để nghe tiếp): " + parseEx.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        if (isListening) System.out.println("Ngắt kết nối luồng Socket ngầm: " + e.getMessage());
                    }
                })
                .start();
    }

    // 🔥 SỬA ĐỔI: Nhận thêm biến cờ isAutoBidTriggered để lập lịch hiển thị Text cực kỳ trực quan
    public void onNewBidBroadcastReceived(
            int incomingAuctionId, BigDecimal newPrice, String bidderName, LocalDateTime newEndTime, boolean isAutoBidTriggered) {

        if (this.currentAuction != null && incomingAuctionId == this.currentAuction.getAuctionId()) {
            Platform.runLater(
                    () -> {
                        this.currentMaxPrice = newPrice;
                        lblCurrentPrice.setText(String.format("%,d VND", newPrice.longValue()));
                        lblHighestBidder.setText(bidderName);

                        bidStepCount++;
                        priceSeries.getData().add(new XYChart.Data<>(bidStepCount, newPrice.doubleValue()));

                        // CHUẨN HOÁ HIỂN THỊ: Nếu giá tăng do Bot, thêm nhãn nhận diện tránh nghi ngờ hack hệ thống
                        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        String historyLine;
                        if (isAutoBidTriggered) {
                            historyLine = String.format("[🤖 AutoBid] [%s] %s đã đặt %,d VND", time, bidderName, newPrice.longValue());
                        } else {
                            historyLine = String.format("[%s] %s đã đặt %,d VND", time, bidderName, newPrice.longValue());
                        }
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

                                        if (currentAuction.getStatus() == org.example.core.shared.enums.AuctionStatus.RUNNING) {
                                            currentAuction.setStatus(org.example.core.shared.enums.AuctionStatus.FINISHED);
                                            lblStatus.setText("FINISHED");
                                            updateUiComponentsByStatus(org.example.core.shared.enums.AuctionStatus.FINISHED);
                                            String currentWinner = lblHighestBidder.getText().trim();
                                            if (currentWinner.equals("Chưa có") || currentWinner.equals("--")) {
                                                showWinnerBox("Không có ai");
                                            } else {
                                                showWinnerBox(currentWinner);
                                            }
                                        }
                                        stopTimer(); // Tắt luôn cái vòng lặp đếm ngược này đi cho đỡ tốn RAM
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

    // 🔥 CẬP NHẬT: Code hủy diệt phòng của Admin
    @FXML
    public void handleForceCancel(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("CẢNH BÁO QUẢN TRỊ VIÊN");
        confirm.setHeaderText("Hủy phiên đấu giá ID: " + currentAuctionId + "?");
        confirm.setContentText("Hành động này sẽ đóng phiên ngay lập tức. Toàn bộ giao dịch sẽ bị vô hiệu hóa! Không thể hoàn tác!");

        confirm.showAndWait().ifPresent(responseBtn -> {
            if (responseBtn == ButtonType.OK) {
                try {
                     System.out.println("Đang gửi lệnh ADMIN_CANCEL_AUCTION lên Server cho ID: " + currentAuctionId);
                    Request cancelReq = new Request("ADMIN_CANCEL_AUCTION", currentAuctionId);

                    // 4. Gửi qua luồng Socket Live của phòng
                    if (outToServer != null) {
                        outToServer.println(gson.toJson(cancelReq));
                        // Khi Server xử lý xong, nó sẽ gửi tín hiệu "AUCTION_ENDED" hoặc "CANCELED" về cho hàm listenFromServer() để đổi UI cho tất cả mọi người.
                    }
                } catch (Exception e) {
                    showAlert("Lỗi Hệ Thống", "Không thể gửi lệnh Hủy: " + e.getMessage());
                }
            }
        });
    }
    // 🔥 HÀM XỬ LÝ HIỂN THỊ ALERT VÀ ĐẾM NGƯỢC 15 GIÂY
    private void showCancelAlertAndCountdown() {
        stopTimer(); // Dừng bộ đếm thời gian đấu giá cũ lại
        updateUiComponentsByStatus(org.example.core.shared.enums.AuctionStatus.FINISHED); // Khóa hết các nút bấm nhập giá

        // Tạo Alert cảnh báo
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("PHÒNG ĐÃ BỊ HỦY KHẨN CẤP");
        alert.setHeaderText("Thông báo từ Ban Quản Trị Hệ Thống");

        // Thiết lập bộ đếm 15 giây chạy ngầm
        ScheduledExecutorService countdownService = Executors.newSingleThreadScheduledExecutor();
        final int[] timeLeft = {15};

        countdownService.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                if (timeLeft[0] > 0) {
                    alert.setContentText("Phiên đấu giá này đã bị Admin hủy bỏ khẩn cấp!\n" +
                            "Hệ thống sẽ tự động đưa bạn về Danh mục sau: " + timeLeft[0] + " giây.");
                    timeLeft[0]--;
                } else {
                    // Hết 15 giây: Tắt luồng đếm, đóng alert và chuyển scene
                    countdownService.shutdown();
                    alert.close();
                    cleanUpBeforeExit();
                    switchScene(new ActionEvent(btnPlaceBid, null), "/views/AuctionCatalogView.fxml", "Danh mục đấu giá");
                }
            });
        }, 0, 1, TimeUnit.SECONDS);

        // Nếu người dùng chủ động click nút OK trên thông báo trước 15s
        alert.showAndWait().ifPresent(buttonType -> {
            countdownService.shutdown(); // Tắt luồng đếm ngầm lập tức
            cleanUpBeforeExit();
            switchScene(new ActionEvent(btnPlaceBid, null), "/views/AuctionCatalogView.fxml", "Danh mục đấu giá");
        });
    }
}