package org.example.client.controllers.auction;

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

import org.example.client.controllers.BaseController;
import org.example.client.controllers.user.UserSidebarController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.AuctionSession;
import org.example.client.utils.ImageUtils;
import org.example.client.utils.UserSession;
import org.example.core.dto.bidDTO.BidBroadcastDTO;
import org.example.core.dto.bidDTO.BidRequestDTO;
import org.example.core.dto.bidDTO.AutoBidRequestDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.models.items.Item;
import org.example.core.models.users.User;
import org.example.core.shared.enums.ActionType;
import org.example.core.shared.enums.AuctionStatus;

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller trung tâm quản lý phòng đấu giá thời gian thực (Auction Room). Hỗ trợ giao tiếp Socket
 * song công, đặt giá trực tiếp, cài đặt robot tự động trả giá (AutoBid) và đồ thị biến động giá
 * trực quan.
 */
public class AuctionRoomController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(AuctionRoomController.class.getName());

  @FXML private Label lblItemName, lblTimer, lblStatus, lblBid;
  @FXML private Label lblCurrentPrice, lblHighestBidder, lblWinner, lblBidError;
  @FXML private Label lblOnlineCount;
  @FXML private TextArea taDescription;
  @FXML private TextField tfBidAmount;
  @FXML private ListView<String> lvBidHistory;
  @FXML private LineChart<Number, Number> lineChart;
  @FXML private Button btnPlaceBid;
  @FXML private ImageView ivItemImage;
  @FXML private VBox vboxBidderControls;
  @FXML private VBox vboxSellerControls;
  @FXML private VBox vboxAdminControls;
  @FXML private TextField tfMaxBid;
  @FXML private Button btnToggleAutoBid;
  @FXML private VBox vboxPriceBox;
  @FXML private VBox vboxWinnerBox;
@FXML private Button btnCheckout;
  private XYChart.Series<Number, Number> priceSeries;
  private ScheduledExecutorService timerService;
  private Auction currentAuction;
  private BigDecimal currentMaxPrice;
  private int bidStepCount = 0;
  private boolean isAutoBidActive = false;
  private Gson gson;
  private int currentAuctionId;
  private int currentUserId;
  private PrintWriter outToServer;
  private BufferedReader inFromServer;
  private volatile boolean isListening = true;

  /**
   * Khởi tạo giao diện phòng đấu giá, sửa lỗi lệch trạng thái client-server và kích hoạt kết nối bộ
   * đàm ngầm.
   */
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

    if (sessionAuction != null) {
      LocalDateTime now = LocalDateTime.now();
      if ("OPEN".equals(sessionAuction.getStatus().name())
          && now.isAfter(sessionAuction.getStartTime())) {
        if (now.isBefore(sessionAuction.getEndTime())) {
          sessionAuction.setStatus(org.example.core.shared.enums.AuctionStatus.RUNNING);
          logger.info("[UI SYNC] Đã tự động đồng bộ ép trạng thái phòng sang RUNNING.");
        } else {
          sessionAuction.setStatus(org.example.core.shared.enums.AuctionStatus.FINISHED);
          logger.info("[UI SYNC] Đã tự động đồng bộ ép trạng thái phòng sang FINISHED.");
        }
      }
    }

    if (sessionAuction != null && sessionItem != null) {
      setupRoom(sessionAuction, sessionItem);
      Auction freshAuction = AuctionSession.getInstance().getCurrentAuction();
      if (freshAuction != null && "FINISHED".equals(freshAuction.getStatus().name())) {
        Platform.runLater(
            () -> {
              tfMaxBid.setDisable(true);
              btnToggleAutoBid.setDisable(true);
              tfBidAmount.setDisable(true);
              btnPlaceBid.setDisable(true);

              btnToggleAutoBid.setText("PHÒNG ĐÃ ĐÓNG");
              btnToggleAutoBid.setStyle(
                  "-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold;");

              if (freshAuction.getHighestBidderName() != null
                  && !freshAuction.getHighestBidderName().isEmpty()) {
                lblHighestBidder.setText(
                    "Người thắng cuộc: " + freshAuction.getHighestBidderName());
              } else {
                lblHighestBidder.setText("Người thắng cuộc: Không có ai tham gia");
              }

              BigDecimal finalPrice =
                  freshAuction.getHighestBid() != null
                      ? freshAuction.getHighestBid()
                      : freshAuction.getItem().getStartingPrice();
              lblCurrentPrice.setText(String.format("%,d VNĐ", finalPrice.longValue()));
            });
      }
      listenFromServer();
    } else {
      showAlert("Lỗi", "Không tìm thấy dữ liệu phòng đấu giá!");
    }
  }

  /** Xử lý sự kiện đặt giá thủ công từ người tham gia đấu giá. */
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

      BidRequestDTO bidReq =
          new BidRequestDTO(currentAuctionId, currentUserId, bidAmount, myUsername);
      Request requestContainer = new Request(ActionType.PLACE_BID, bidReq);

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

  /** Xử lý cơ chế bật/tắt Bot tự động nâng giá (AutoBid Engine). */
  @FXML
  private void handleToggleAutoBid(ActionEvent event) {
    lblBidError.setText("");
    if (!isAutoBidActive) {
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

        AutoBidRequestDTO autoBidReq =
            new AutoBidRequestDTO(currentAuctionId, currentUserId, maxBid);
        outToServer.println(gson.toJson(new Request(ActionType.REGISTER_AUTOBID, autoBidReq)));

//        isAutoBidActive = true;
//        tfMaxBid.setDisable(true);
//        btnToggleAutoBid.setText("HỦY AUTOBID");
//        btnToggleAutoBid.setStyle(
//            "-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;");
        lblBidError.setStyle("-fx-text-fill: green;");
        lblBidError.setText("Hệ thống AutoBid đã được kích hoạt thành công!");

      } catch (NumberFormatException e) {
        lblBidError.setStyle("-fx-text-fill: red;");
        lblBidError.setText("Số tiền cài trần không hợp lệ!");
      }
    } else {
      AutoBidRequestDTO cancelReq =
          new AutoBidRequestDTO(currentAuctionId, currentUserId, BigDecimal.ZERO);
      outToServer.println(gson.toJson(new Request(ActionType.CANCEL_AUTOBID, cancelReq)));

      isAutoBidActive = false;
      tfMaxBid.setDisable(false);
      tfMaxBid.clear();
      tfMaxBid.setPromptText("Nhập hạn mức trần...");
      btnToggleAutoBid.setText("KÍCH HOẠT AUTOBID");
      btnToggleAutoBid.setStyle(
          "-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold;-fx-background-radius: 6;");
      lblBidError.setStyle("-fx-text-fill: green;");
      lblBidError.setText("Hệ thống AutoBid đã ngừng hoạt động!");
    }
  }

  /** Quyền lực Admin: Cưỡng chế đóng phòng, hủy phiên đấu giá khẩn cấp. */
  @FXML
  public void handleForceCancel(ActionEvent event) {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("CẢNH BÁO QUẢN TRỊ VIÊN");
    confirm.setHeaderText("Hủy phiên đấu giá ID: " + currentAuctionId + "?");
    confirm.setContentText(
        "Hành động này sẽ đóng phiên ngay lập tức. Toàn bộ giao dịch sẽ bị vô hiệu hóa! Không thể hoàn tác!");

    confirm
        .showAndWait()
        .ifPresent(
            responseBtn -> {
              if (responseBtn == ButtonType.OK) {
                try {
                  logger.log(
                      Level.INFO,
                      "Đang gửi lệnh ADMIN_CANCEL_AUCTION lên Server cho ID: {0}",
                      currentAuctionId);
                  Request cancelReq = new Request(ActionType.ADMIN_CANCEL_AUCTION, currentAuctionId);
                  if (outToServer != null) {
                    outToServer.println(gson.toJson(cancelReq));
                  }
                } catch (Exception e) {
                  showAlert("Lỗi Hệ Thống", "Không thể gửi lệnh Hủy: " + e.getMessage());
                }
              }
            });
  }

  @FXML
  private void handleBackToCatalog(ActionEvent event) {
    cleanUpBeforeExit();
    switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục đấu giá");
  }

  @FXML
  public void handleCheckout(ActionEvent event) {
    cleanUpBeforeExit();
    UserSidebarController.currentView = "WaiPaymentView.fxml";
    switchScene(event, "/views/WaitPaymentView.fxml", "Sản phẩm chờ thanh toán");
  }
  /** Dựng khung phòng đấu giá dựa trên dữ liệu Auction truyền từ màn hình chính. */
  private void setupRoom(Auction auction, Item item) {
    this.currentAuction = auction;
    this.currentAuctionId = auction.getAuctionId();
    User user = UserSession.getInstance().getCurrentUser();
    setupRoleBasedUI(user, auction);
    this.currentMaxPrice =
        auction.getHighestBid() != null
            ? auction.getHighestBid()
            : (item != null ? item.getStartingPrice() : BigDecimal.ZERO);

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
          logger.log(Level.SEVERE, "Lỗi hiển thị hình ảnh trong phòng đấu giá", e);
        }
      }
    } else {
      lblItemName.setText(
          auction.getItemName() != null ? auction.getItemName() : "Sản phẩm không xác định");
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
            String.format(
                "[%s] %s đã đặt %,d VND", time, bid.getBidderName(), bid.getAmount().longValue());
        lvBidHistory.getItems().add(0, historyLine);
      }
      String topBidder =
          auction.getBidHistory().get(auction.getBidHistory().size() - 1).getBidderName();
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
      Request joinReq = new Request(ActionType.JOIN_ROOM, joinData);
      outToServer.println(gson.toJson(joinReq));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi gửi gói tin JOIN_ROOM lên Server", e);
    }

    updateUiComponentsByStatus(auction.getStatus());
    startCountdown();
  }

  /** Luồng lắng nghe Socket ngầm nhận các sự kiện phát sóng (Broadcast) từ máy chủ. */
  private void listenFromServer() {
    new Thread(() -> {
      logger.info("Đã kích hoạt luồng lắng nghe bộ đàm ngầm...");
      try {
        String messageFromServer;
        while (isListening && (messageFromServer = inFromServer.readLine()) != null) {
          if (!isListening) break;

          try {
            Response response = gson.fromJson(messageFromServer, Response.class);
            if (response == null || response.getStatus() == null) continue;

            switch (response.getStatus()) {
              case "NEW_BID"          -> handleNewBidBroadcast(response);
              case "SUCCESS"          -> handleSuccessResponse(response);
              case "ERROR"            -> handleErrorResponse(response);
              case "ERROR_BID"        -> handleBidErrorResponse(response);
              case "AUCTION_STARTED"  -> handleAuctionStartedBroadcast(response);
              case "AUCTION_ENDED"    -> handleAuctionEndedBroadcast(response);
              case "MY_AUTOBID_STATUS"-> handleMyAutoBidStatusResponse(response);
              case "AUTOBID_DISABLED" -> handleAutoBidDisabledBroadcast(response);
              case "ROOM_USER_COUNT"  -> handleRoomUserCountBroadcast(response);
              case "INITIAL_ROOM_DATA"-> handleInitialRoomDataResponse(response);
              default -> logger.warning("Nhận được gói tin không rõ trạng thái: " + response.getStatus());
            }
          } catch (Exception parseEx) {
            logger.log(Level.WARNING, "Lỗi bóc tách gói tin JSON truyền xuống từ Server", parseEx);
          }
        }
      } catch (Exception e) {
        if (isListening) logger.log(Level.SEVERE, "Ngắt kết nối luồng Socket ngầm trong phòng đấu giá", e);
      }
    }).start();
  }

  /** Đồng bộ giao diện khi nhận được sự kiện có lượt trả giá mới thành công từ Server. */
  public void onNewBidBroadcastReceived(
      int incomingAuctionId,
      BigDecimal newPrice,
      String bidderName,
      LocalDateTime newEndTime,
      boolean isAutoBidTriggered) {
    if (this.currentAuction != null && incomingAuctionId == this.currentAuction.getAuctionId()) {
      Platform.runLater(
          () -> {
            this.currentMaxPrice = newPrice;
            lblCurrentPrice.setText(String.format("%,d VND", newPrice.longValue()));
            lblHighestBidder.setText(bidderName);

            bidStepCount++;
            priceSeries.getData().add(new XYChart.Data<>(bidStepCount, newPrice.doubleValue()));

            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String historyLine =
                isAutoBidTriggered
                    ? String.format(
                        "[AutoBid] [%s] %s đã đặt %,d VND",
                        time, bidderName, newPrice.longValue())
                    : String.format(
                        "[%s] %s đã đặt %,d VND", time, bidderName, newPrice.longValue());
            lvBidHistory.getItems().add(0, historyLine);

            lblBidError.setText("");
            if (newEndTime != null) {
              this.currentAuction.setEndTime(newEndTime);
              startCountdown();
            }
          });
    }
  }

  /** Kích hoạt đồng hồ đếm ngược real-time đến lúc kết thúc phiên hoặc mở cửa phòng. */
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
                  LocalDateTime targetTime =
                      (currentAuction.getStatus()
                              == org.example.core.shared.enums.AuctionStatus.OPEN)
                          ? currentAuction.getStartTime()
                          : currentAuction.getEndTime();

                  if (targetTime == null) {
                    lblTimer.setText("--:--:--");
                    return;
                  }

                  Duration duration = Duration.between(now, targetTime);

                  if (duration.isNegative() || duration.isZero()) {
                    lblTimer.setText("00:00:00");

                    if (currentAuction.getStatus()
                        == org.example.core.shared.enums.AuctionStatus.RUNNING) {
                      currentAuction.setStatus(
                          org.example.core.shared.enums.AuctionStatus.FINISHED);
                      lblStatus.setText("FINISHED");
                      updateUiComponentsByStatus(
                          org.example.core.shared.enums.AuctionStatus.FINISHED);
                      String currentWinner = lblHighestBidder.getText().trim();
                      showWinnerBox(
                          currentWinner.equals("Chưa có") || currentWinner.equals("--")
                              ? "Không có ai"
                              : currentWinner);
                    }
                    stopTimer();
                  } else {
                    lblTimer.setText(
                        String.format(
                            "%02d:%02d:%02d",
                            duration.toHours(),
                            duration.toMinutesPart(),
                            duration.toSecondsPart()));
                  }
                } catch (Exception e) {
                  logger.log(Level.WARNING, "Cảnh báo nhẹ luồng đếm ngược thời gian phòng", e);
                  lblTimer.setText("--:--:--");
                }
              });
        },
        0,
        1,
        TimeUnit.SECONDS);
  }

  /** Phân luồng hiển thị bảng điều khiển dựa trên quyền hạn (Bidder / Seller / Admin). */
  private void setupRoleBasedUI(User currentUser, Auction auction) {
    vboxBidderControls.setVisible(false);
    vboxBidderControls.setManaged(false);
    vboxSellerControls.setVisible(false);
    vboxSellerControls.setManaged(false);
    vboxAdminControls.setVisible(false);
    vboxAdminControls.setManaged(false);

    if (currentUser == null) return;
    String role = currentUser.getRole().toString();
    int userId = currentUser.getUserId();

    if (role != null && role.equalsIgnoreCase("ADMIN")) {
      vboxAdminControls.setVisible(true);
      vboxAdminControls.setManaged(true);
    } else if (userId == auction.getOwnerId()) {
      vboxSellerControls.setVisible(true);
      vboxSellerControls.setManaged(true);
    } else {
      vboxBidderControls.setVisible(true);
      vboxBidderControls.setManaged(true);
    }
  }

  /** Bật hộp hiển thị tên người chiến thắng chung cuộc khi kết thúc phiên đấu giá. */
  private void showWinnerBox(String winnerName) {
    vboxPriceBox.setVisible(false);
    vboxPriceBox.setManaged(false);
    vboxWinnerBox.setVisible(true);
    vboxWinnerBox.setManaged(true);
    lblWinner.setText(winnerName != null && !winnerName.isEmpty() ? winnerName : "Không có ai");
  }

  private void updatePriceUI(BigDecimal price, String bidder) {
    lblCurrentPrice.setText(String.format("%,d VND", price.longValue()));
    lblHighestBidder.setText(bidder);
  }

  private void updateUiComponentsByStatus(org.example.core.shared.enums.AuctionStatus status) {
    boolean disable =
        (status == org.example.core.shared.enums.AuctionStatus.OPEN
            || status == org.example.core.shared.enums.AuctionStatus.FINISHED);
    tfBidAmount.setDisable(disable);
    btnPlaceBid.setDisable(disable);
    tfMaxBid.setDisable(disable || isAutoBidActive);
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

  /** Dừng đếm ngược, hủy đăng ký lắng nghe và giải phóng phiên trước khi thoát phòng. */
  private void cleanUpBeforeExit() {
    stopTimer();
    try {
      Request dummyReq = new Request(ActionType.LEAVE_ROOM, null);
      if (outToServer != null) {
        outToServer.println(gson.toJson(dummyReq));
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi gửi lệnh đóng/rời phòng LEAVE_ROOM lên Server", e);
    }
    isListening = false;
    AuctionSession.getInstance().clearSession();
  }

  private void stopTimer() {
    if (timerService != null && !timerService.isShutdown()) {
      timerService.shutdown();
    }
  }

  /**
   * Thiết lập cảnh báo và đếm ngược tự động đá người dùng về Catalog khi Admin hủy phòng khẩn cấp.
   */
  private void showCancelAlertAndCountdown() {
    stopTimer();
    updateUiComponentsByStatus(org.example.core.shared.enums.AuctionStatus.FINISHED);

    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("PHÒNG ĐÃ BỊ HỦY KHẨN CẤP");
    alert.setHeaderText("Thông báo từ Ban Quản Trị Hệ Thống");

    ScheduledExecutorService countdownService = Executors.newSingleThreadScheduledExecutor();
    final int[] timeLeft = {15};

    countdownService.scheduleAtFixedRate(
        () ->
            Platform.runLater(
                () -> {
                  if (timeLeft[0] > 0) {
                    alert.setContentText(
                        "Phiên đấu giá này đã bị Admin hủy bỏ khẩn cấp!\nHệ thống sẽ tự động đưa bạn về Danh mục sau: "
                            + timeLeft[0]
                            + " giây.");
                    timeLeft[0]--;
                  } else {
                    countdownService.shutdown();
                    alert.close();
                    cleanUpBeforeExit();
                    switchScene(
                        new ActionEvent(btnPlaceBid, null),
                        "/views/AuctionCatalogView.fxml",
                        "Danh mục đấu giá");
                  }
                }),
        0,
        1,
        TimeUnit.SECONDS);

    alert
        .showAndWait()
        .ifPresent(
            buttonType -> {
              countdownService.shutdown();
              cleanUpBeforeExit();
              switchScene(
                  new ActionEvent(btnPlaceBid, null),
                  "/views/AuctionCatalogView.fxml",
                  "Danh mục đấu giá");
            });
  }
  private void handleNewBidBroadcast(Response response) {
    if (response.getData() == null) return;
    String innerData = gson.toJson(response.getData());
    BidBroadcastDTO data = gson.fromJson(innerData, BidBroadcastDTO.class);
    if (data != null) {
      onNewBidBroadcastReceived(data.getAuctionId(), BigDecimal.valueOf(data.getNewPrice()),
              data.getLeaderUsername(), data.getNewEndTime(), data.isAutoBidTriggered());
    }
  }

  private void handleSuccessResponse(Response response) {
    if ("Kích hoạt hệ thống AutoBid gác phòng thành công!".equals(response.getMessage())) {
      Platform.runLater(() -> {
        isAutoBidActive = true;
        tfMaxBid.setDisable(true);
        btnToggleAutoBid.setText("HỦY AUTOBID");
        btnToggleAutoBid.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;-fx-background-radius:6");
        lblBidError.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        lblBidError.setText("Hệ thống AutoBid đã được kích hoạt thành công!");
      });
    }
  }

  private void handleErrorResponse(Response response) {
    if (response.getMessage() != null && response.getMessage().contains("AutoBid")) {
      Platform.runLater(() -> {
        isAutoBidActive = false;
        tfMaxBid.setDisable(false);
        btnToggleAutoBid.setText("KÍCH HOẠT AUTOBID");
        btnToggleAutoBid.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold;-fx-background-radius:6");
        lblBidError.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        lblBidError.setText(response.getMessage());
      });
    }
  }

  private void handleBidErrorResponse(Response response) {
    Platform.runLater(() -> {
      lblBidError.setStyle("-fx-text-fill: red;");
      int code = response.getData() instanceof Number ? ((Number) response.getData()).intValue() : -1;
      String prefix = (code == 4003) ? "[Lỗi giá] " : (code == 4001) ? "[Ví điện tử] " : "[" + code + "] ";
      lblBidError.setText(prefix + response.getMessage());
    });
  }

  private void handleAuctionStartedBroadcast(Response response) {
    Platform.runLater(() -> {
      if (currentAuction != null) currentAuction.setStatus(AuctionStatus.RUNNING);
      lblStatus.setText("RUNNING");
      updateUiComponentsByStatus(AuctionStatus.RUNNING);
      lblBidError.setStyle("-fx-text-fill: green;");
      lblBidError.setText(response.getMessage());
      startCountdown();
    });
  }

  private void handleAuctionEndedBroadcast(Response response) {
    String additionalData = response.getData() != null ? response.getData().toString().replace("\"", "") : "";
    if ("ADMIN_CANCELLED".equals(additionalData)) {
      Platform.runLater(this::showCancelAlertAndCountdown);
    } else {
      String winnerName = response.getMessage();
      Platform.runLater(() -> {
        stopTimer();
        lblTimer.setText("00:00:00");
        lblStatus.setText("FINISHED");
        showWinnerBox(winnerName);
        lblWinner.setText(winnerName != null ? winnerName : "Không có");
        updateUiComponentsByStatus(AuctionStatus.FINISHED);

        User user = UserSession.getInstance().getCurrentUser();
        if (winnerName != null && winnerName.equals(user.getUserName())) {
          showAlert("Thông báo", "CHÚC MỪNG! BẠN ĐÃ TRỞ THÀNH CHỦ NHÂN CỦA MÓN ĐỒ!");
          btnCheckout.setVisible(true);
          btnCheckout.setManaged(true);
        } else {
          showAlert("Thông báo", "Phiên đấu giá đã kết thúc!\nNgười chiến thắng: " + winnerName);
        }
      });
    }
  }

  private void handleMyAutoBidStatusResponse(Response response) {
    try {
      String innerData = gson.toJson(response.getData());
      java.util.Map<String, Double> dataMap = gson.fromJson(innerData, java.util.Map.class);
      double savedMaxBid = dataMap.get("maxBid");

      Platform.runLater(() -> {
        isAutoBidActive = true;
        tfMaxBid.setText(String.format("%.0f", savedMaxBid));
        tfMaxBid.setDisable(true);
        btnToggleAutoBid.setText("HỦY AUTOBID");
        btnToggleAutoBid.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;");
        lblBidError.setStyle("-fx-text-fill: green;");
        lblBidError.setText("Hệ thống nhận diện: Bot AutoBid của bạn đang gác phòng này!");
      });
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi phân tích trạng thái AutoBid cũ từ máy chủ", e);
    }
  }

  private void handleAutoBidDisabledBroadcast(Response response) {
    try {
      int disabledUserId = (int) Double.parseDouble(response.getData().toString());
      if (currentUserId == disabledUserId) {
        Platform.runLater(() -> {
          isAutoBidActive = false;
          tfMaxBid.setDisable(false);
          tfMaxBid.clear();
          tfMaxBid.setPromptText("Nhập hạn mức trần...");
          btnToggleAutoBid.setText("KÍCH HOẠT AUTOBID");
          btnToggleAutoBid.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-weight: bold;-fx-background-radius: 6;");
          lblBidError.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;");
          lblBidError.setText("⚠️ Bot đã tự động tắt do mức giá vượt quá trần!");
        });
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi phân tích dữ liệu tắt AutoBid", e);
    }
  }

  private void handleRoomUserCountBroadcast(Response response) {
    String countStr = response.getMessage();
    Platform.runLater(() -> lblOnlineCount.setText("| 👥 Đang xem: " + countStr + " người"));
  }

  private void handleInitialRoomDataResponse(Response response) {
    try {
      Auction freshAuction = gson.fromJson(gson.toJson(response.getData()), Auction.class);
      Platform.runLater(() -> {
        AuctionSession.getInstance().setCurrentAuction(freshAuction);
        BigDecimal finalPrice = freshAuction.getHighestBid() != null ? freshAuction.getHighestBid() : freshAuction.getItem().getStartingPrice();
        lblCurrentPrice.setText(String.format("%,d VNĐ", finalPrice.longValue()));

        if ("FINISHED".equals(freshAuction.getStatus().name())) {
          tfMaxBid.setDisable(true);
          btnToggleAutoBid.setDisable(true);
          tfBidAmount.setDisable(true);
          btnPlaceBid.setDisable(true);
          btnToggleAutoBid.setText("PHÒNG ĐÃ ĐÓNG");
          btnToggleAutoBid.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold;");

          if (freshAuction.getHighestBidderName() != null && !freshAuction.getHighestBidderName().isEmpty()) {
            lblHighestBidder.setText("Người thắng cuộc: " + freshAuction.getHighestBidderName());
          } else {
            lblHighestBidder.setText("Người thắng cuộc: Không có ai tham gia");
          }
        } else {
          if (freshAuction.getHighestBidderName() != null && !freshAuction.getHighestBidderName().isEmpty()) {
            lblHighestBidder.setText(freshAuction.getHighestBidderName());
          } else {
            lblHighestBidder.setText("Chưa có");
          }
        }
      });
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi dựng lại UI phòng đấu giá từ dữ liệu khởi tạo", e);
    }
  }
  @FXML
  private void handleQuickBidStep(ActionEvent event) {
    if (currentAuction == null || currentAuction.getBidIncrement() == null) return;
    BigDecimal step = currentAuction.getBidIncrement();
    BigDecimal newAmount = currentMaxPrice.add(step);
    tfBidAmount.setText(newAmount.toPlainString());
  }

  @FXML
  private void handleQuickBid50(ActionEvent event) {
    BigDecimal newAmount = currentMaxPrice.add(new BigDecimal("50000"));
    tfBidAmount.setText(newAmount.toPlainString());
  }

  @FXML
  private void handleQuickBid100(ActionEvent event) {
    BigDecimal newAmount = currentMaxPrice.add(new BigDecimal("100000"));
    tfBidAmount.setText(newAmount.toPlainString());
  }

  @FXML
  private void handleQuickBid500(ActionEvent event) {
    BigDecimal newAmount = currentMaxPrice.add(new BigDecimal("500000"));
    tfBidAmount.setText(newAmount.toPlainString());
  }
}
