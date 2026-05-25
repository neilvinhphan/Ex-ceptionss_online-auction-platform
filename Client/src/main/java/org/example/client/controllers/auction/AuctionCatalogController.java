package org.example.client.controllers.auction;

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
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import org.example.client.controllers.BaseController;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller quản lý màn hình danh mục đấu giá (Catalog). Hỗ trợ tìm kiếm, lọc theo giá, trạng
 * thái, thể loại và nâng cấp tài khoản lên Seller.
 */
public class AuctionCatalogController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(AuctionCatalogController.class.getName());

  @FXML private FlowPane auctionFlowPane;
  @FXML private HBox upgradeBanner;
  @FXML private RadioButton rbStatusAll, rbStatusOpen, rbStatusRunning, rbStatusFinished;
  @FXML private CheckBox cbTypeAll, cbTypeElectronics, cbTypeVehicle, cbTypeArt;
  @FXML private RadioButton rbPriceAll, rbPrice1, rbPrice2, rbPrice3, rbPrice4, rbPrice5;
  @FXML private RadioButton rbSortNewest, rbSortOldest;
  @FXML private Button btnGoToHistory;

  private List<Auction> allAuctionsList = new ArrayList<>();
  private final List<Timeline> activeTimelines = new ArrayList<>();
  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();
  private final DateTimeFormatter timeFormatter =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  /**
   * Khởi tạo giao diện, kiểm tra quyền hạn người dùng để hiển thị banner nâng cấp và tải danh sách
   * phòng.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    User currentUser = UserSession.getInstance().getCurrentUser();

    if (currentUser != null) {
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

    if (rbStatusFinished != null) {
      rbStatusFinished.setVisible(false);
      rbStatusFinished.setManaged(false);
    }
  }

  /** Xử lý sự kiện khi người dùng yêu cầu nâng cấp lên tài khoản Seller. */
  @FXML
  private void handleUpgrade(ActionEvent event) {
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle("Xác nhận nâng cấp");
    dialog.setHeaderText(
        "Bạn có chắc chắn muốn nâng cấp lên tài khoản SELLER không?\nNếu có, vui lòng nhập lại mật khẩu để xác nhận:");

    ButtonType confirmButtonType = new ButtonType("Nâng cấp", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

    PasswordField passwordField = new PasswordField();
    passwordField.setPromptText("Nhập mật khẩu hiện tại của bạn...");

    VBox vbox = new VBox();
    vbox.setSpacing(10);
    vbox.getChildren().add(passwordField);
    dialog.getDialogPane().setContent(vbox);

    Platform.runLater(passwordField::requestFocus);

    dialog.setResultConverter(
        dialogButton -> dialogButton == confirmButtonType ? passwordField.getText() : null);

    Optional<String> result = dialog.showAndWait();
    result.ifPresent(
        password -> {
          try {
            User currentUser = UserSession.getInstance().getCurrentUser();
            int currentId = currentUser.getUserId();

            logger.info("Bắt đầu quy trình kiểm tra mật khẩu nâng cấp.");

            if (currentUser.getPassword() == null || currentUser.getPassword().isEmpty()) {
              showAlert(
                  "Lỗi hệ thống",
                  "Dữ liệu User trong Session không chứa mật khẩu mã hóa. Vui lòng kiểm tra lại API Login phía Server!");
              return;
            }

            if (!BCrypt.checkpw(password, currentUser.getPassword())) {
              showAlert("Lỗi", "Sai mật khẩu!");
              return;
            }

            logger.info("Xác thực BCrypt thành công. Tiến hành gửi yêu cầu lên máy chủ.");
            sendUpgradeRequestToServer(currentId, event);

          } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi trong quá trình xác thực mật khẩu bằng BCrypt", e);
            showAlert("Lỗi vặt", "Có lỗi xảy ra khi kiểm tra mật khẩu: " + e.getMessage());
          }
        });
  }

  /** Xử lý sự kiện lọc và sắp xếp danh sách đấu giá theo các tiêu chí đã chọn. */
  @FXML
  public void handleFilter(ActionEvent event) {
    if (allAuctionsList == null || allAuctionsList.isEmpty()) return;
    List<Auction> filteredList = new ArrayList<>();

    for (Auction auction : allAuctionsList) {
      Item item = auction.getItem();
      if (item == null) continue;

      boolean matchType = false;
      if (cbTypeAll.isSelected()) {
        matchType = true;
      } else {
        if (cbTypeElectronics.isSelected() && item instanceof ElectronicsItem) matchType = true;
        if (cbTypeVehicle.isSelected() && item instanceof VehicleItem) matchType = true;
        if (cbTypeArt.isSelected() && item instanceof ArtItem) matchType = true;
      }
      if (!matchType) continue;

      boolean matchStatus = false;
      if (rbStatusAll.isSelected()) {
        matchStatus = true;
      } else if (rbStatusOpen.isSelected() && auction.getStatus() == AuctionStatus.OPEN) {
        matchStatus = true;
      } else if (rbStatusRunning.isSelected() && auction.getStatus() == AuctionStatus.RUNNING) {
        matchStatus = true;
      }
      if (!matchStatus) continue;

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

    filteredList.sort(
        (a1, a2) -> {
          LocalDateTime t1 = a1.getStartTime();
          LocalDateTime t2 = a2.getStartTime();

          if (t1 == null && t2 == null)
            return Integer.compare(a1.getAuctionId(), a2.getAuctionId());
          if (t1 == null) return 1;
          if (t2 == null) return -1;

          return rbSortNewest.isSelected() ? t2.compareTo(t1) : t1.compareTo(t2);
        });

    displayAuctions(filteredList);
  }

  /** Chuyển hướng người dùng sang màn hình xem lịch sử giao dịch thị trường. */
  @FXML
  private void handleGoToHistory(ActionEvent event) {
    try {
      switchScene(event, "/views/MarketHistoryView.fxml", "Lịch sử thị trường VET");
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Không thể chuyển từ Catalog sang trang Lịch sử thị trường", e);
    }
  }

  /** Gửi gói tin nâng cấp quyền người dùng lên máy chủ qua luồng kết nối Socket. */
  private void sendUpgradeRequestToServer(int userId, ActionEvent event) {
    UpdateRoleRequestDTO updateRoleRequestDTO = new UpdateRoleRequestDTO(userId);
    Request request = new Request("UPDATE_ROLE", updateRoleRequestDTO);
    String jsonRequest = gson.toJson(request);

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
                logger.log(Level.SEVERE, "Lỗi kết nối Socket khi gửi yêu cầu nâng cấp role", e);
              }
            })
        .start();
  }

  /** Tải danh sách phòng đấu giá đang mở hoặc sắp diễn ra từ máy chủ. */
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
                    logger.warning(
                        "Server báo SUCCESS dữ liệu danh sách phòng đấu giá trả về bị rỗng (Null).");
                    return;
                  }
                  String jsonData = gson.toJson(response.getData());
                  JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
                  List<Auction> fetchedAuctions = new ArrayList<>();

                  logger.log(
                      Level.INFO,
                      "Tìm thấy {0} phần tử đấu giá thô nhận về từ máy chủ.",
                      jsonArray.size());
                  for (JsonElement element : jsonArray) {
                    JsonObject auctionObj = element.getAsJsonObject();
                    Auction auction = gson.fromJson(auctionObj, Auction.class);

                    if (auctionObj.has("type") && !auctionObj.get("type").isJsonNull()) {
                      String type = auctionObj.get("type").getAsString();

                      Item parsedItem =
                          switch (type.toUpperCase()) {
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
                      if (auction.getStatus() == AuctionStatus.OPEN
                          || auction.getStatus() == AuctionStatus.RUNNING) {
                        fetchedAuctions.add(auction);
                      } else {
                        logger.log(
                            Level.INFO,
                            "Đã dọn dẹp phòng {0} khỏi danh sách vì trạng thái: {1}",
                            new Object[] {auction.getAuctionId(), auction.getStatus()});
                      }
                    } else {
                      logger.log(
                          Level.WARNING,
                          "Bỏ qua phòng đấu giá ID {0} do lỗi ép kiểu thông tin sản phẩm.",
                          auction.getAuctionId());
                    }
                  }
                  allAuctionsList = new ArrayList<>(fetchedAuctions);
                  Platform.runLater(
                      () -> {
                        displayAuctions(allAuctionsList);
                        handleFilter(new ActionEvent());
                      });
                }
              } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi", "Mất kết nối: " + e.getMessage()));
                logger.log(
                    Level.SEVERE, "Lỗi xảy ra trong quá trình nạp danh sách đấu giá hoạt động", e);
              }
            })
        .start();
  }

  /** Hiển thị danh sách các thẻ phòng đấu giá lên giao diện FlowPane. */
  private void displayAuctions(List<Auction> auctionsToDisplay) {
    clearActiveTimelines();
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
  }

  /** Tạo giao diện thẻ card độc lập cho từng sản phẩm đấu giá. */
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
    infoBox.setSpacing(8.0);
    infoBox.setStyle("-fx-padding: 15;");

    Label lblName = new Label(item.getItemName());
    lblName.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #333;");
    lblName.setWrapText(true);

    long currentPrice =
        (auction.getHighestBid() != null)
            ? auction.getHighestBid().longValue()
            : ((auction.getItem() != null && auction.getItem().getStartingPrice() != null)
                ? auction.getItem().getStartingPrice().longValue()
                : 0);

    Label lblPrice = new Label("Giá hiện tại: " + String.format("%,d", currentPrice) + " đ");
    lblPrice.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #e53935;");

    String startStr =
        (auction.getStartTime() != null)
            ? auction.getStartTime().format(timeFormatter)
            : "--/--/---- --:--:--";
    Label lblStartTime = new Label("⏱ Bắt đầu: " + startStr);
    lblStartTime.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");

    Label lblCountdown = new Label("⏳ Đang tính toán...");
    Label lblState = new Label();
    Button btnJoin = new Button();
    btnJoin.setMaxWidth(Double.MAX_VALUE);
    btnJoin.setStyle(
        "-fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");

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

    setupCountdownEngine(auction, lblCountdown, lblState, btnJoin);
    btnJoin.setOnAction(e -> handleJoinAuction(e, auction));

    infoBox.getChildren().addAll(lblName, lblPrice, lblStartTime, lblCountdown, lblState, btnJoin);
    card.getChildren().addAll(imageContainer, infoBox);

    return card;
  }

  /** Thiết lập hệ thống đếm ngược thời gian thực (mỗi 1 giây) cho từng thẻ card đấu giá. */
  private void setupCountdownEngine(
      Auction auction, Label lblCountdown, Label lblState, Button btnJoin) {
    Timeline timeline =
        new Timeline(
            new KeyFrame(
                Duration.seconds(1),
                ev -> {
                  LocalDateTime now = LocalDateTime.now();

                  if (auction.getStatus() == AuctionStatus.OPEN) {
                    if (auction.getStartTime() != null) {
                      if (now.isBefore(auction.getStartTime())) {
                        java.time.Duration duration =
                            java.time.Duration.between(now, auction.getStartTime());
                        lblCountdown.setText(
                            String.format(
                                "⏳ Khai mạc sau: %02d:%02d:%02d",
                                duration.toHours(),
                                duration.toMinutesPart(),
                                duration.toSecondsPart()));
                        lblCountdown.setStyle(
                            "-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #0288D1;");
                      } else {
                        if (auction.getEndTime() != null && now.isBefore(auction.getEndTime())) {
                          java.time.Duration durationToEnd =
                              java.time.Duration.between(now, auction.getEndTime());
                          lblCountdown.setText(
                              String.format(
                                  "🛑 Còn lại: %02d:%02d:%02d",
                                  durationToEnd.toHours(),
                                  durationToEnd.toMinutesPart(),
                                  durationToEnd.toSecondsPart()));
                          lblCountdown.setStyle(
                              "-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #e53935;");

                          lblState.setText("🔥 ĐANG ĐẤU GIÁ");
                          lblState.setStyle(
                              "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #4caf50;");
                          btnJoin.setText("THAM GIA NGAY");
                          btnJoin.setStyle(
                              "-fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5; -fx-background-color: #28a745;");
                        } else {
                          setCardToFinished(lblCountdown, lblState, btnJoin);
                        }
                      }
                    } else {
                      lblCountdown.setText("⏳ Thời gian chưa xác định");
                      lblCountdown.setStyle(
                          "-fx-font-size: 13; -fx-font-style: italic; -fx-text-fill: #78909C;");
                    }
                  } else if (auction.getStatus() == AuctionStatus.RUNNING) {
                    if (auction.getEndTime() != null) {
                      if (now.isBefore(auction.getEndTime())) {
                        java.time.Duration duration =
                            java.time.Duration.between(now, auction.getEndTime());
                        lblCountdown.setText(
                            String.format(
                                "🛑 Còn lại: %02d:%02d:%02d",
                                duration.toHours(),
                                duration.toMinutesPart(),
                                duration.toSecondsPart()));
                        lblCountdown.setStyle(
                            "-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #e53935;");
                      } else {
                        setCardToFinished(lblCountdown, lblState, btnJoin);
                      }
                    } else {
                      lblCountdown.setText("🛑 Thời gian kết thúc chưa rõ");
                      lblCountdown.setStyle(
                          "-fx-font-size: 13; -fx-font-style: italic; -fx-text-fill: #78909C;");
                    }
                  } else {
                    setCardToFinished(lblCountdown, lblState, btnJoin);
                  }
                }));

    timeline.setCycleCount(Animation.INDEFINITE);
    timeline.play();
    activeTimelines.add(timeline);
  }

  /** Chuyển trạng thái hiển thị của một thẻ card đấu giá sang trạng thái Đã kết thúc. */
  private void setCardToFinished(Label lblCountdown, Label lblState, Button btnJoin) {
    lblCountdown.setText("🛑 Phiên đấu giá đã khép lại!");
    lblCountdown.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #78909C;");
    lblState.setText("🏁 ĐÃ KẾT THÚC");
    lblState.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #78909C;");
    btnJoin.setText("PHÒNG ĐÃ ĐÓNG");
    btnJoin.setStyle(
        "-fx-background-color: #B0BEC5; -fx-text-fill: white; -fx-background-radius: 5;");
  }

  /** Điều hướng người dùng vào chi tiết phòng đấu giá được lựa chọn. */
  private void handleJoinAuction(ActionEvent event, Auction auction) {
    try {
      AuctionSession.getInstance().setCurrentAuction(auction);
      AuctionSession.getInstance().setCurrentItem(auction.getItem());
      switchScene(
          event,
          "/views/AuctionRoomView.fxml",
          "Phòng đấu giá: " + auction.getItem().getItemName());
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Gặp lỗi khi cố gắng chuyển hướng vào phòng đấu giá", ex);
      showAlert("Lỗi", "Không thể vào phòng đấu giá: " + ex.getMessage());
    }
  }

  /** Đồng bộ hóa và gom nhóm các nút chọn bộ lọc (ToggleGroup). */
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

  /** Giải phóng bộ nhớ, tắt tất cả luồng chạy ngầm của bộ đếm thời gian. */
  private void clearActiveTimelines() {
    for (Timeline timeline : activeTimelines) {
      if (timeline != null) {
        timeline.stop();
      }
    }
    activeTimelines.clear();
  }
}
