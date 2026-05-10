package org.example.client.controllers;

import com.google.gson.Gson;
import com.google.gson.internal.bind.util.ISO8601Utils;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.AuctionSession;
import org.example.client.utils.ImageUtils;
import org.example.client.utils.UserSession;
import org.example.core.dto.BidBroadcastDTO;
import org.example.core.dto.BidRequestDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.entities.Auction;
import org.example.core.models.items.Item;

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
  @FXML private TextArea taDescription;
  @FXML private TextField tfBidAmount;
  @FXML private ListView<String> lvBidHistory;
  @FXML private LineChart<Number, Number> lineChart;
  @FXML private Button btnPlaceBid;
  @FXML private ImageView ivItemImage;
  private XYChart.Series<Number, Number> priceSeries;
  private ScheduledExecutorService timerService;

  private Auction currentAuction;
  private BigDecimal currentMaxPrice;
  private int bidStepCount = 0;

  private Gson gson;
  private int currentAuctionId;
  private int currentUserId;

  // --- Đồ nghề Socket ---
  private PrintWriter outToServer;
  private BufferedReader inFromServer;
  private volatile boolean isListening = true; // Cờ để tắt luồng nghe khi thoát phòng

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    if (UserSession.getInstance().getCurrentUser() != null) {
      this.currentUserId = UserSession.getInstance().getCurrentUser().getUserId();
    }

    // [ĐÃ SỬA LỖI]: Không lấy currentAuctionId ở đây vì currentAuction đang bị null!

    // 1. Khởi tạo biểu đồ
    priceSeries = new XYChart.Series<>();
    priceSeries.setName("Biến động giá");
    lineChart.getData().add(priceSeries);

    // 2. Lấy Gson và Socket từ ClientManager
    gson = ClientManager.getInstance().getGson();

    // Móc cái AuctionClient ra trước
    AuctionClient clientSocket = ClientManager.getInstance().getClient();

    // Rồi mới lấy ống in/out từ clientSocket
    outToServer = clientSocket.getOut();
    inFromServer = clientSocket.getIn();

    // 3. LẤY DỮ LIỆU TỪ TRẠM TRUNG CHUYỂN
    Auction sessionAuction = AuctionSession.getInstance().getCurrentAuction();
    Item sessionItem = AuctionSession.getInstance().getCurrentItem();

    System.out.println(
        "DEBUG: Auction có null ko? " + sessionAuction.getBidderId() + (sessionAuction == null));
    System.out.println("DEBUG: Item có null ko? " + (sessionItem == null));

    if (sessionAuction != null && sessionItem != null) {
      // Setup giao diện ngay lập tức
      setupRoom(sessionAuction, sessionItem);

      // 4. MỞ BỘ ĐÀM LẮNG NGHE SERVER
      listenFromServer();
    } else {
      showAlert("Lỗi", "Không tìm thấy dữ liệu phòng đấu giá!");
    }
  }

  private void setupRoom(Auction auction, Item item) {
    this.currentAuction = auction;
    // [ĐÃ FIX]: Gán ID phòng ở đây sau khi auction đã có dữ liệu thật
    this.currentAuctionId = auction.getAuctionId();
    this.currentMaxPrice =
        auction.getHighestBid() != null ? auction.getHighestBid() : item.getStartingPrice();

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
    if (auction.getBidIncrement() != null) {
      lblBid.setText(String.format("%,d VND", auction.getBidIncrement().longValue()));
    } else {
      lblBid.setText("K co du lieu");
    }
    lblStatus.setText(
        auction.getStatus() != null ? auction.getStatus().toString() : "k co du lieu");
    lblWinner.setText("--");

    // 1. Mặc định ban đầu cứ cho là "Chưa có"
    String topBidder = "Chưa có";
    // 2. Kiểm tra xem lịch sử đặt giá đã có cái nào chưa?
    if (auction.getBidHistory() != null && !auction.getBidHistory().isEmpty()) {
      int lastIndex = auction.getBidHistory().size() - 1;
      topBidder = auction.getBidHistory().get(lastIndex).getBidderName();
    }
    System.out.println(auction.getBidHistory());
    System.out.println(auction.getBidderId());
    System.out.println(topBidder);

    // 3. Cập nhật lên Giao diện
    updatePriceUI(currentMaxPrice, topBidder);
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
        lblBidError.setStyle("-fx-text-fill: red;");
        lblBidError.setText("Giá đặt phải cao hơn giá hiện tại!");
        return;
      }

      // ĐÓNG GÓI DỮ LIỆU ĐỂ GỬI QUA SOCKET
      BidRequestDTO bidReq = new BidRequestDTO(currentAuctionId, currentUserId, bidAmount);
      Request requestContainer = new Request("PLACE_BID", bidReq);

      // Gửi đi
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
      e.printStackTrace();
    }
  }

  /** LUỒNG CHẠY NGẦM LẮNG NGHE REAL-TIME TỪ SERVER */
  private void listenFromServer() {
    new Thread(
            () -> {
              System.out.println("Đã vào luồng lắng nghe Server...");
              try {
                String messageFromServer;
                while (isListening && (messageFromServer = inFromServer.readLine()) != null) {

                  // Thấy cờ ngắt là tự sát luôn, không đọc cố
                  if (!isListening) break;

                  try {
                    Response response = gson.fromJson(messageFromServer, Response.class);

                    // NHẬN BROADCAST CÓ NGƯỜI ĐẶT GIÁ THÀNH CÔNG
                    if ("NEW_BID".equals(response.getStatus())) {

                      // Bóc data bên trong
                      String innerData = gson.toJson(response.getData());
                      BidBroadcastDTO data = gson.fromJson(innerData, BidBroadcastDTO.class);

                      int aId = data.getAuctionId();
                      BigDecimal price = BigDecimal.valueOf(data.getNewPrice());
                      String leader = data.getLeaderUsername();
                      LocalDateTime endT = data.getNewEndTime();

                      System.out.println(
                          "[BROADCAST NHẬN ĐƯỢC] Giá: " + price + " | Leader: " + leader);

                      // Ném cho UI xử lý
                      onNewBidBroadcastReceived(aId, price, leader, endT);
                    }
                    // NHẬN THÔNG BÁO LỖI KHI MÌNH ĐẶT GIÁ SAI
                    else if ("ERROR_BID".equals(response.getStatus())) {
                      Platform.runLater(
                          () -> {
                            lblBidError.setStyle("-fx-text-fill: red;");
                            lblBidError.setText(response.getMessage());
                          });
                    }
                  } catch (Exception parseEx) {
                    System.err.println("Lỗi bóc tách Gson: " + parseEx.getMessage());
                  }
                }
              } catch (Exception e) {
                if (isListening) System.out.println("Mất kết nối Server: " + e.getMessage());
              }
            })
        .start();
  }

  // Các hàm cập nhật UI được gọi từ luồng lắng nghe
  // Thêm tham số LocalDateTime newEndTime vào hàm này
  public void onNewBidBroadcastReceived(
      int incomingAuctionId, BigDecimal newPrice, String bidderName, LocalDateTime newEndTime) {

    // Cú chốt chặn: Chỉ cập nhật nếu gói tin này thuộc về đúng phòng mình đang đứng
    if (this.currentAuction != null && incomingAuctionId == this.currentAuction.getAuctionId()) {

      // BẮT BUỘC DÙNG RUN LATER KHI ĐỤNG VÀO GIAO DIỆN
      Platform.runLater(
          () -> {

            // 1. Cập nhật biến nhớ giá trị Max
            this.currentMaxPrice = newPrice;

            // 2. [CẬP NHẬT GÓC PHẢI TRÊN] Label Người dẫn đầu & Giá
            lblCurrentPrice.setText(String.format("%,d VND", newPrice.longValue()));
            lblHighestBidder.setText(bidderName);

            // 3. [CẬP NHẬT BIỂU ĐỒ] Vẽ thêm 1 điểm nốt ruồi mới
            bidStepCount++;
            priceSeries.getData().add(new XYChart.Data<>(bidStepCount, newPrice.doubleValue()));

            // 4. [CẬP NHẬT LỊCH SỬ] Nhét 1 dòng text lên ĐẦU danh sách ListView
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String historyLine =
                String.format("[%s] %s đã đặt %,d VND", time, bidderName, newPrice.longValue());
            lvBidHistory
                .getItems()
                .add(0, historyLine); // add(0, ...) để thằng mới nhất bị đẩy lên trên cùng

            // 5. Dọn dẹp câu báo lỗi cũ (nếu có) cho nó sạch mắt
            lblBidError.setText("");

            // 6. Cập nhật đồng hồ (Nếu có luật Anti-Sniping gia hạn thêm giờ)
            if (newEndTime != null) {
              this.currentAuction.setEndTime(newEndTime);
              startCountdown(newEndTime); // Reset lại đồng hồ đếm ngược
            }
          });
    }
  }

  public void onAuctionEndBroadcastReceived(
      int incomingAuctionId, String winnerName, BigDecimal finalPrice) {
    if (this.currentAuction != null && incomingAuctionId == this.currentAuction.getAuctionId()) {
      Platform.runLater(
          () -> {
            stopTimer();
            lblTimer.setText("00:00:00");
            lblStatus.setText("FINISHED");
            lblWinner.setText(winnerName);
            btnPlaceBid.setDisable(true);
            tfBidAmount.setDisable(true);
            if (winnerName.equals(UserSession.getInstance().getCurrentUser().getUserName())) {
              showAlert("Thông báo", "CHÚC MỪNG! BẠN ĐÃ TRỞ THÀNH CHỦ NHÂN CỦA MÓN ĐỒ!");
            } else {
              showAlert(
                  "Thông báo", "Phiên đấu giá đã kết thúc!\nNgười chiến thắng: " + winnerName);
            }
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
    timerService.scheduleAtFixedRate(
        () -> {
          Duration duration = Duration.between(LocalDateTime.now(), endTime);
          Platform.runLater(
              () -> {
                if (duration.isNegative() || duration.isZero()) {
                  stopTimer();
                  lblTimer.setText("00:00:00");
                  lblStatus.setText("FINISHED");
                  btnPlaceBid.setDisable(true);
                  tfBidAmount.setDisable(true);
                  // Hien thi nguoi chien thang
                  String winner = lblHighestBidder.getText();
                  lblWinner.setText(winner);

                } else {
                  long h = duration.toHours();
                  long m = duration.toMinutesPart();
                  long s = duration.toSecondsPart();
                  lblTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
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
    switchScene(event, "/views/WaitPaymentView.fxml", "San pham cho thanh toan");
  }

  @FXML
  private void handleMain(ActionEvent event) {
    cleanUpBeforeExit();
    switchScene(event, "/views/MainView.fxml", "Trang chủ");
  }

  private void cleanUpBeforeExit() {
    stopTimer();

    // 1. Gửi cục xương (Chim mồi) lên Server TRƯỚC
    try {
      org.example.core.dto.Request dummyReq = new org.example.core.dto.Request("LEAVE_ROOM", null);
      if (outToServer != null) {
        outToServer.println(gson.toJson(dummyReq));
      }
    } catch (Exception e) {
      System.out.println("Lỗi gửi chim mồi: " + e.getMessage());
    }

    // 2. Hạ cờ để vòng lặp tự ngắt khi nhận được cục xương
    isListening = false;

    // 3. Clear bộ nhớ
    AuctionSession.getInstance().clearSession();
  }

  private void stopTimer() {
    if (timerService != null && !timerService.isShutdown()) {
      timerService.shutdown();
    }
  }
}
