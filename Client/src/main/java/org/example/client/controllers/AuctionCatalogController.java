package org.example.client.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.AuctionSession;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.userDTO.UpdateRoleRequestDTO;
import org.example.core.models.entities.Auction;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;
import org.example.client.utils.ImageUtils;
import org.example.core.models.users.User;
import org.example.core.shared.enums.AuctionStatus;
import org.example.core.shared.enums.RoleType;
import org.mindrot.jbcrypt.BCrypt;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionCatalogController extends BaseController implements Initializable {

  @FXML private FlowPane auctionFlowPane;
  @FXML
  private HBox upgradeBanner;

  private List<Auction> allAuctionsList = new ArrayList<>();
  // Khai báo một danh sách các Timeline để quản lý và xóa bỏ bộ đếm cũ khi tải lại trang, tránh rò rỉ bộ nhớ
  private List<Timeline> activeTimelines = new ArrayList<>();
  @FXML private RadioButton rbStatusAll, rbStatusOpen, rbStatusRunning, rbStatusFinished;
  @FXML private CheckBox cbTypeAll, cbTypeElectronics, cbTypeVehicle, cbTypeArt;
  @FXML private RadioButton rbPriceAll, rbPrice1, rbPrice2, rbPrice3, rbPrice4, rbPrice5;
  @FXML private RadioButton rbSortNewest, rbSortOldest;
  @FXML private Button btnGoToHistory;
  private Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();
  private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    User currentUser = UserSession.getInstance().getCurrentUser();

    if (currentUser != null) {
      // --- KIỂM TRA ROLE ĐỂ ẨN/HIỆN BANNER NÂNG CẤP ---
      if (currentUser.getRole() == RoleType.ADMIN || currentUser.getRole() == RoleType.SELLER) {
        upgradeBanner.setVisible(false);
        upgradeBanner.setManaged(false);
      } else {
        upgradeBanner.setVisible(true);
        upgradeBanner.setManaged(true);
      }
    }
    setupToggleGroups();
    loadActiveAuctions();

    // 🔥 DỌN DẸP GIAO DIỆN: Xóa sổ nút Lọc "Đã kết thúc" khỏi màn hình Catalog
    if (rbStatusFinished != null) {
      rbStatusFinished.setVisible(false);
      rbStatusFinished.setManaged(false); // Rút phần không gian của nó trên layout luôn
    }
  }
  @FXML
  private void handleUpgrade(ActionEvent event) {
    // 1. Tạo hộp thoại Custom
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle("Xác nhận nâng cấp");
    dialog.setHeaderText(
            "Bạn có chắc chắn muốn nâng cấp lên tài khoản SELLER không?\nNếu có, vui lòng nhập lại mật khẩu để xác nhận:");

    // 2. Tạo các nút bấm (Xác nhận / Hủy)
    ButtonType confirmButtonType = new ButtonType("Nâng cấp", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

    // 3. Tạo trường nhập mật khẩu (PasswordField ẩn ký tự)
    PasswordField passwordField = new PasswordField();
    passwordField.setPromptText("Nhập mật khẩu hiện tại của bạn...");

    // 4. Đưa trường nhập vào Giao diện hộp thoại
    VBox vbox = new VBox();
    vbox.setSpacing(10);
    vbox.getChildren().add(passwordField);
    dialog.getDialogPane().setContent(vbox);

    // Xử lý focus mặc định vào trường mật khẩu khi mở popup
    javafx.application.Platform.runLater(passwordField::requestFocus);

    // 5. Bắt sự kiện khi bấm nút "Nâng cấp"
    dialog.setResultConverter(
            dialogButton -> {
              if (dialogButton == confirmButtonType) {
                return passwordField.getText();
              }
              return null;
            });

    // 6. Hiển thị hộp thoại và chờ người dùng nhập
    Optional<String> result = dialog.showAndWait();

    // 7. Xử lý kết quả sau khi nhập
    // 7. Xử lý kết quả sau khi nhập
    result.ifPresent(
            password -> {
              try {
                User currentUser = UserSession.getInstance().getCurrentUser();
                int currentId = currentUser.getUserId();

                System.out.println("--- BẮT ĐẦU KIỂM TRA MẬT KHẨU ---");
                System.out.println("Pass nhập vào: " + password);
                System.out.println("Pass trong Session (Hash): " + currentUser.getPassword());

                // Chặn lỗi nếu Session không lưu password
                if (currentUser.getPassword() == null || currentUser.getPassword().isEmpty()) {
                  showAlert("Lỗi hệ thống", "Dữ liệu User trong Session không chứa mật khẩu mã hóa. Vui lòng kiểm tra lại API Login phía Server!");
                  return;
                }

                if (!BCrypt.checkpw(password, currentUser.getPassword())) {
                  showAlert("Lỗi", "Sai mật khẩu!");
                  return;
                }

                System.out.println("=> CHECK BCRYPT THÀNH CÔNG. Chuẩn bị gửi Server...");
                sendUpgradeRequestToServer(currentId, event);

              } catch (Exception e) {
                System.out.println("=> VĂNG LỖI NGẦM TRONG LÚC CHECK BCRYPT:");
                e.printStackTrace();
                showAlert("Lỗi vặt", "Có lỗi xảy ra khi kiểm tra mật khẩu: " + e.getMessage());
              }
            });
  }

  private void sendUpgradeRequestToServer(int userId, ActionEvent event) {
    // 1. Lấy thông tin user đang login

    // 2. Gói vào DTO (ví dụ UpgradeRoleDTO gồm username và password)
    UpdateRoleRequestDTO updateRoleRequestDTO = new UpdateRoleRequestDTO(userId);
    // 3. Gửi Socket lên Server
    Request request = new Request("UPDATE_ROLE", updateRoleRequestDTO);
    String jsonRequest = gson.toJson(request);
    // 4. Nhận phản hồi:
    //    - Nếu Server báo "SUCCESS" -> Chúc mừng, báo đăng nhập lại để cập nhật menu.
    //    - Nếu Server báo "ERROR" (sai pass) -> In ra Alert lỗi.
    new Thread(
            () -> {
              try {
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);
                Platform.runLater(
                        () -> {
                          if ("SUCCESS".equals(response.getStatus())) {
                            showAlert("Chúc mừng", "Đăng nhập lại để cập nhật menu.");
                            UserSession.getInstance().cleanUserSession();
                            switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
                          } else {
                            showAlert("Đã xảy ra lỗi", "Mật khẩu không chính xác! Vui lòng nhập lại.");
                          }
                        });
              } catch (Exception e) {
                Platform.runLater(
                        () -> showAlert("Lỗi kết nối", "Không thể gửi yêu cầu: " + e.getMessage()));
                e.printStackTrace();
              }
            })
            .start();
  }
  private void clearActiveTimelines() {
    for (Timeline timeline : activeTimelines) {
      if (timeline != null) {
        timeline.stop();
      }
    }
    activeTimelines.clear();
  }

  private void displayAuctions(List<Auction> auctionsToDisplay) {
    clearActiveTimelines(); // Dọn dẹp sạch sẽ các bộ đếm giây cũ trước khi vẽ khung mới
    auctionFlowPane.getChildren().clear();

    for (Auction auction : auctionsToDisplay) {
      VBox card = createAuctionCard(auction);
      auctionFlowPane.getChildren().add(card);

      String base64 = auction.getItem().getImage();
      if (base64 != null && !base64.isEmpty()) {
        new Thread(
                () -> {
                  Image img = ImageUtils.decodeBase64ToImage(base64);
                  Platform.runLater(
                          () -> {
                            ImageView iv = (ImageView) card.lookup(".auction-image");
                            if (iv != null && img != null) iv.setImage(img);
                          });
                })
                .start();
      }
    }
//    for (Auction auction : auctionsToDisplay) {
//      VBox card = createAuctionCard(auction);
//      auctionFlowPane.getChildren().add(card);
//
//      String base64 = auction.getItem().getImage();
//      if (base64 != null && !base64.isEmpty()) {
//        new Thread(
//                () -> {
//                  Image img = ImageUtils.decodeBase64ToImage(base64);
//                  Platform.runLater(
//                          () -> {
//                            ImageView iv = (ImageView) card.lookup(".auction-image");
//                            if (iv != null && img != null) iv.setImage(img);
//                          });
//                })
//                .start();
//      }
//    }


  }
  private void loadActiveAuctions() {
    Request request = new Request("GET_ACTIVE_AUCTIONS", null);
    String jsonRequest = gson.toJson(request);
    new Thread(
            () -> {
              try {
                 String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);

                if ("SUCCESS".equals(response.getStatus())) {
                  if (response.getData() == null) {
                    System.out.println("CẢNH BÁO: Server báo SUCCESS nhưng Data bị Null!");
                    return;
                  }
                  String jsonData = gson.toJson(response.getData());
                  JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
                  List<Auction> fetchedAuctions = new ArrayList<>();

                  System.out.println("Số lượng phần tử trong mảng JSON: " + jsonArray.size());
                  for (JsonElement element : jsonArray) {
                    JsonObject auctionObj = element.getAsJsonObject();
                    Auction auction = gson.fromJson(auctionObj, Auction.class);

                    if (auctionObj.has("type") && !auctionObj.get("type").isJsonNull()) {
                      String type = auctionObj.get("type").getAsString();

                      Item parsedItem = switch (type.toUpperCase()) {
                        case "ART" -> gson.fromJson(auctionObj, ArtItem.class);
                        case "ELECTRONICS" -> gson.fromJson(auctionObj, ElectronicsItem.class);
                        case "VEHICLE" -> gson.fromJson(auctionObj, VehicleItem.class);
                        default -> null;
                      };

                      if (parsedItem != null) {
                        if (auctionObj.has("itemName")) {
                          parsedItem.setItemName(auctionObj.get("itemName").getAsString());
                        }
                        auction.setItem(parsedItem);
                      }
                    }

                    if (auction.getItem() != null) {
                      // 🔥 BÚNG TAY THANOS: Chỉ nạp các phòng Đang chờ (OPEN) hoặc Đang chạy (RUNNING)
                      if (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING) {
                        fetchedAuctions.add(auction);
                      } else {
                        System.out.println("🧹 Đã dọn dẹp phòng " + auction.getAuctionId() + " khỏi Catalog vì trạng thái: " + auction.getStatus());
                      }
                    } else {
                      System.out.println("⚠️ CẢNH BÁO: Bỏ qua Auction ID " + auction.getAuctionId() + " do ép kiểu Item thất bại.");
                    }
                  }
                  allAuctionsList = new ArrayList<>(fetchedAuctions);
                  System.out.println("Số lượng Auction lấy được sau khi ép kiểu: " + allAuctionsList.size());
                  Platform.runLater(
                          () -> {
                           displayAuctions(allAuctionsList);
                            handleFilter(new ActionEvent());
                          });
                }
              } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi", "Mất kết nối: " + e.getMessage()));
                e.printStackTrace();
              }
            })
            .start();
  }

  private VBox createAuctionCard(Auction auction) {
    Item item = auction.getItem();

    VBox card = new VBox();
    card.setPrefWidth(310.0);
    card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");

    StackPane imageContainer = new StackPane();
    imageContainer.setPrefHeight(180.0);
    imageContainer.setStyle("-fx-background-color: #ECEFF1; -fx-background-radius: 10 10 0 0;");

    ImageView imageView = new ImageView();
    imageView.getStyleClass().add("auction-image");

    if (imageView.getImage() == null) {
      Label imgLabel = new Label("No Image Available");
      imgLabel.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 14; -fx-font-style: italic;");
      imageContainer.getChildren().add(imgLabel);
    }

    imageView.setFitWidth(310.0);
    imageView.setFitHeight(180.0);
    imageView.setPreserveRatio(true);
    imageView.setSmooth(true);

    imageContainer.getChildren().add(imageView);

    VBox infoBox = new VBox();
    infoBox.setSpacing(8.0); // Giảm một xíu spacing để vừa vặn các trường thời gian
    infoBox.setStyle("-fx-padding: 15;");

    Label lblName = new Label(item.getItemName());
    lblName.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #333;");
    lblName.setWrapText(true);

    long currentPrice = (auction.getHighestBid() != null) ? auction.getHighestBid().longValue() : ((auction.getItem() != null && auction.getItem().getStartingPrice() != null) ? auction.getItem().getStartingPrice().longValue() : 0);

    Label lblPrice = new Label("Giá hiện tại: " + String.format("%,d", currentPrice) + " đ");
    lblPrice.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #e53935;");

    // ==========================================
    // BỔ SUNG: KHỞI TẠO CÁC TRƯỜNG THỜI GIAN
    // ==========================================
    String startStr = (auction.getStartTime() != null) ? auction.getStartTime().format(timeFormatter) : "--/--/---- --:--:--";
    Label lblStartTime = new Label("⏱ Bắt đầu: " + startStr);
    lblStartTime.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");

    // Nhãn để thực hiện đếm ngược thời gian động từng giây
    Label lblCountdown = new Label("⏳ Đang tính toán...");

    Label lblState = new Label();
    Button btnJoin = new Button();
    btnJoin.setMaxWidth(Double.MAX_VALUE);
    btnJoin.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");

    if (auction.getStatus() == AuctionStatus.OPEN) {
      lblState.setText("⏳ SẮP DIỄN RA");
      lblState.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ff9800;");
      btnJoin.setText("VÀO PHÒNG CHỜ");
      btnJoin.setStyle(btnJoin.getStyle() + "-fx-background-color: #ff9800;");
    } else {
      lblState.setText("🔥 ĐANG ĐẤU GIÁ");
      lblState.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #4caf50;");
      btnJoin.setText("THAM GIA NGAY");
      btnJoin.setStyle(btnJoin.getStyle() + "-fx-background-color: #28a745;");
    }

    // ==========================================
    // LOGIC ĐẾM NGƯỢC THỜI GIAN ĐỘNG (REAL-TIME)
    // ==========================================
    setupCountdownEngine(auction, lblCountdown, lblState, btnJoin);

    btnJoin.setOnAction(e -> handleJoinAuction(e, auction));

    // Thêm các nhãn thời gian mới thiết lập vào infoBox để hiển thị lên Card
    infoBox.getChildren().addAll(lblName, lblPrice, lblStartTime, lblCountdown, lblState, btnJoin);
    card.getChildren().addAll(imageContainer, infoBox);

    return card;
  }
/**
   * Bộ xử lý thiết lập bộ đếm thời gian lặp lại mỗi 1 giây cho từng sản phẩm
   * Đã được cấu hình đổi màu chữ động theo trạng thái đếm ngược
   */
  private void setupCountdownEngine(Auction auction, Label lblCountdown, Label lblState, Button btnJoin) {
    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
      LocalDateTime now = LocalDateTime.now();

      if (auction.getStatus() == AuctionStatus.OPEN) {
        // ==========================================
        // TRƯỜNG HỢP PHÒNG SẮP DIỄN RA (OPEN)
        // ==========================================
        if (auction.getStartTime() != null) {
          if (now.isBefore(auction.getStartTime())) {
            // 1. Thực sự CHƯA ĐẾN GIỜ -> Đếm ngược đến lúc KHAI MẠC (Màu xanh dương)
            java.time.Duration duration = java.time.Duration.between(now, auction.getStartTime());
            lblCountdown.setText(String.format("⏳ Khai mạc sau: %02d:%02d:%02d",
                    duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart()));

            // Ép màu XANH DƯƠNG chuẩn UI
            lblCountdown.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #0288D1;");

          } else {
            // 2. QUÁ GIỜ BẮT ĐẦU -> Đếm ngược đến lúc KẾT THÚC (Màu đỏ)
            if (auction.getEndTime() != null && now.isBefore(auction.getEndTime())) {
              java.time.Duration durationToEnd = java.time.Duration.between(now, auction.getEndTime());
              lblCountdown.setText(String.format("🛑 Còn lại: %02d:%02d:%02d",
                      durationToEnd.toHours(), durationToEnd.toMinutesPart(), durationToEnd.toSecondsPart()));

              // Ép sang màu ĐỎ HOÀN TOÀN vì đang đếm thời gian kết thúc
              lblCountdown.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #e53935;");

              // Cập nhật nhãn trạng thái và nút bấm đồng bộ sang ĐANG ĐẤU GIÁ
              lblState.setText("🔥 ĐANG ĐẤU GIÁ");
              lblState.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #4caf50;");
              btnJoin.setText("THAM GIA NGAY");
              btnJoin.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5; -fx-background-color: #28a745;");
            } else {
              // Quá hạn cả giờ kết thúc
              lblCountdown.setText("🛑 Phiên đấu giá đã khép lại!");
              lblCountdown.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #78909C;"); // Màu xám bế mạc
              lblState.setText("🏁 ĐÃ KẾT THÚC");
              lblState.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #78909C;");
           //   btnJoin.setDisable(true);
              btnJoin.setText("PHÒNG ĐÃ ĐÓNG");
              btnJoin.setStyle("-fx-background-color: #B0BEC5; -fx-text-fill: white; -fx-background-radius: 5;");
            }
          }
        } else {
          lblCountdown.setText("⏳ Thời gian chưa xác định");
          lblCountdown.setStyle("-fx-font-size: 13; -fx-font-style: italic; -fx-text-fill: #78909C;");
        }
      } else if (auction.getStatus() == AuctionStatus.RUNNING) {
        // ==========================================
        // TRƯỜNG HỢP PHÒNG ĐANG ĐẤU GIÁ (RUNNING)
        // ==========================================
        if (auction.getEndTime() != null) {
          if (now.isBefore(auction.getEndTime())) {
            // Đang đếm ngược thời gian kết thúc -> Ép sang màu ĐỎ rực lửa
            java.time.Duration duration = java.time.Duration.between(now, auction.getEndTime());
            lblCountdown.setText(String.format("🛑 Còn lại: %02d:%02d:%02d",
                    duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart()));

            // Ép màu ĐỎ chuẩn UI
            lblCountdown.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #e53935;");

          } else {
            lblCountdown.setText("🛑 Phiên đấu giá đã khép lại!");
            lblCountdown.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #78909C;");
            lblState.setText("🏁 ĐÃ KẾT THÚC");
            lblState.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #78909C;");
          //  btnJoin.setDisable(true);
            btnJoin.setText("PHÒNG ĐÃ ĐÓNG");
            btnJoin.setStyle("-fx-background-color: #B0BEC5; -fx-text-fill: white; -fx-background-radius: 5;");
          }
        } else {
          lblCountdown.setText("🛑 Thời gian kết thúc chưa rõ");
          lblCountdown.setStyle("-fx-font-size: 13; -fx-font-style: italic; -fx-text-fill: #78909C;");
        }
      } else {
        // Các trạng thái đóng cửa hoàn toàn (FINISHED, PAID, CANCELED) -> Chuyển màu XÁM
        lblCountdown.setText("🏁 Phiên đấu giá đã đóng cửa");
        lblCountdown.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #78909C;");
      //  btnJoin.setDisable(true);
        lblState.setText("🏁 ĐÃ KẾT THÚC");
        lblState.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #78909C;");
        btnJoin.setText("PHÒNG ĐÃ ĐÓNG");
        btnJoin.setStyle("-fx-background-color: #B0BEC5; -fx-text-fill: white; -fx-background-radius: 5;");
      }
    }));

    timeline.setCycleCount(Animation.INDEFINITE);
    timeline.play();
    activeTimelines.add(timeline);
  }
  private void handleJoinAuction(ActionEvent event, Auction auction) {
    try {
      AuctionSession.getInstance().setCurrentAuction(auction);
      System.out.println(auction.getOwnerId());
      System.out.println(auction.getAuctionId());
      System.out.println(auction.getItem().getItemName());
      AuctionSession.getInstance().setCurrentItem(auction.getItem());
      switchScene(
              event,
              "/views/AuctionRoomView.fxml",
              "Phòng đấu giá: " + auction.getItem().getItemName());

    } catch (Exception ex) {
      ex.printStackTrace();
      showAlert("Lỗi", "Không thể vào phòng đấu giá: " + ex.getMessage());
    }
  }
  private void setupToggleGroups() {
    ToggleGroup statusGroup = new ToggleGroup();
    rbStatusAll.setToggleGroup(statusGroup);
    rbStatusOpen.setToggleGroup(statusGroup);
    rbStatusRunning.setToggleGroup(statusGroup);

    ToggleGroup priceGroup = new ToggleGroup();
    rbPriceAll.setToggleGroup(priceGroup);
    rbPrice1.setToggleGroup(priceGroup);
    rbPrice2.setToggleGroup(priceGroup);
    rbPrice3.setToggleGroup(priceGroup);
    rbPrice4.setToggleGroup(priceGroup);
    rbPrice5.setToggleGroup(priceGroup);

    ToggleGroup sortGroup = new ToggleGroup();
    rbSortNewest.setToggleGroup(sortGroup);
    rbSortOldest.setToggleGroup(sortGroup);
  }
  @FXML
  public void handleFilter(ActionEvent event) {
    if (allAuctionsList == null || allAuctionsList.isEmpty()) return;
    List<Auction> filteredList = new ArrayList<>();
    for (Auction auction : allAuctionsList) {
      Item item = auction.getItem();
      if (item == null) continue;
// LỌC THEO LOẠI TÀI SẢN
      boolean matchType = false;
      if (cbTypeAll.isSelected()) {
        matchType = true;
      } else {
        if (cbTypeElectronics.isSelected() && item instanceof ElectronicsItem) matchType = true;
        if (cbTypeVehicle.isSelected() && item instanceof VehicleItem) matchType = true;
        if (cbTypeArt.isSelected() && item instanceof ArtItem) matchType = true;
      }
      if (!matchType) continue;
      // LỌC THEO TRẠNG THÁI
      boolean matchStatus = false;
      if (rbStatusAll.isSelected()) {
        matchStatus = true;
      } else if (rbStatusOpen.isSelected() && auction.getStatus() == AuctionStatus.OPEN) {
        matchStatus = true;
      } else if (rbStatusRunning.isSelected() && auction.getStatus() == AuctionStatus.RUNNING) {
        matchStatus = true;
      }

      if (!matchStatus) continue;
      // LỌC THEO GIÁ
      boolean matchPrice = false;
      double price = auction.getHighestBid() != null ? auction.getHighestBid().doubleValue() : 0;

      if (rbPriceAll.isSelected()) {
        matchPrice = true;
      } else if (rbPrice1.isSelected() && price < 50000000) {
        matchPrice = true;
      } else if (rbPrice2.isSelected() && price >= 50000000 && price <= 200000000) {
        matchPrice = true;
      } else if (rbPrice3.isSelected() && price > 200000000 && price <= 500000000) {
        matchPrice = true;
      } else if (rbPrice4.isSelected() && price > 500000000 && price <= 2000000000) {
        matchPrice = true;
      } else if (rbPrice5.isSelected() && price > 2000000000) {
        matchPrice = true;
      }

      if (!matchPrice) continue;

      filteredList.add(auction);
    }
    // LỌC THEO THỜI GIAN
    filteredList.sort((a1, a2) -> {
      LocalDateTime t1 = a1.getStartTime();
      LocalDateTime t2 = a2.getStartTime();

      if (t1 == null && t2 == null) return Integer.compare(a1.getAuctionId(), a2.getAuctionId());
      if (t1 == null) return 1;
      if (t2 == null) return -1;

      if (rbSortNewest.isSelected()) {
        return t2.compareTo(t1); // Mới nhất trước (Giảm dần)
      } else {
        return t1.compareTo(t2); // Cũ nhất trước (Tăng dần)
      }
    });
    displayAuctions(filteredList);
  }

  @FXML
  private void handleGoToHistory(ActionEvent event) {
    try {
      // Sử dụng hàm switchScene có sẵn của BaseController để bay màu sang trang Lịch sử
      switchScene(event, "/views/MarketHistoryView.fxml", "Lịch sử thị trường VET");
    } catch (Exception e) {
      System.err.println("❌ Lỗi không thể chuyển từ Catalog sang trang Lịch sử thị trường: " + e.getMessage());
      e.printStackTrace();
    }
  }
}