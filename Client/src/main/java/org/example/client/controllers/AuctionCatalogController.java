package org.example.client.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.AuctionSession;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.entities.Auction; // Cần import model Auction
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;
import org.example.core.models.users.User;
import org.example.client.utils.ImageUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AuctionCatalogController extends BaseController implements Initializable {

  @FXML private FlowPane auctionFlowPane;

  @FXML private MenuButton menuUser;

  private Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    User currentUser = UserSession.getInstance().getCurrentUser();
    if (currentUser != null) {
      menuUser.setText(currentUser.getUserName());
    }

    loadActiveAuctions();
  }

  private void loadActiveAuctions() {
    Request request = new Request("GET_ACTIVE_AUCTIONS", null);
    String jsonRequest = gson.toJson(request);

    new Thread(
            () -> {
              try {
                System.out.println("Đang xin dữ liệu Các phòng đấu giá...");
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);

                Platform.runLater(
                    () -> {
                      if ("SUCCESS".equals(response.getStatus())) {
                        String jsonData = gson.toJson(response.getData());
                        JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
                        List<Auction> fetchedAuctions = new ArrayList<>();

                        for (JsonElement element : jsonArray) {
                          JsonObject auctionObj = element.getAsJsonObject();

                          // Map dữ liệu cơ bản của Auction
                          Auction auction = gson.fromJson(auctionObj, Auction.class);

                          // Xử lý đa hình cho thuộc tính Item nằm bên trong Auction
                          if (auctionObj.has("item") && !auctionObj.get("item").isJsonNull()) {
                            JsonObject itemObj = auctionObj.getAsJsonObject("item");
                            String type = itemObj.get("type").getAsString();
                            Item parsedItem = null;

                            switch (type.toUpperCase()) {
                              case "ART" -> parsedItem = gson.fromJson(itemObj, ArtItem.class);
                              case "ELECTRONICS" ->
                                  parsedItem = gson.fromJson(itemObj, ElectronicsItem.class);
                              case "VEHICLE" ->
                                  parsedItem = gson.fromJson(itemObj, VehicleItem.class);
                            }
                            // Gắn Item đã parse vào Auction
                            auction.setItem(parsedItem);
                          }

                          if (auction.getItem() != null) {
                            fetchedAuctions.add(auction);
                          }
                        }

                        System.out.println(
                            "Đã tải xong " + fetchedAuctions.size() + " phòng đấu giá.");

                        auctionFlowPane.getChildren().clear();

                        for (Auction auction : fetchedAuctions) {
                          VBox card = createAuctionCard(auction);
                          auctionFlowPane.getChildren().add(card);
                        }

                      } else {
                        showAlert(
                            "Lỗi", "Không thể tải danh sách đấu giá: " + response.getMessage());
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(
                    () -> showAlert("Lỗi kết nối", "Mất kết nối tới máy chủ: " + e.getMessage()));
                e.printStackTrace();
              }
            })
        .start();
  }

  /** Truyền thẳng Auction vào để lấy giá highestBid và dữ liệu Item */
  private VBox createAuctionCard(Auction auction) {
    Item item = auction.getItem();

    VBox card = new VBox();
    card.setPrefWidth(310.0);
    card.setStyle(
        "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");

    // --- PHẦN XỬ LÝ ẢNH TỐI ƯU ---
    // Sử dụng StackPane để ảnh luôn nằm giữa khung hình
    javafx.scene.layout.StackPane imageContainer = new javafx.scene.layout.StackPane();
    imageContainer.setPrefHeight(180.0);
    imageContainer.setStyle("-fx-background-color: #ECEFF1; -fx-background-radius: 10 10 0 0;");

    ImageView imageView = new ImageView();

    // Kiểm tra và lấy ảnh từ Base64
    if (item.getImage() != null && !item.getImage().isEmpty()) {
      Image decodedImage = ImageUtils.decodeBase64ToImage(item.getImage());
      if (decodedImage != null) {
        imageView.setImage(decodedImage);
      }
    }

    // Nếu không có ảnh hoặc lỗi decode, hiển thị placeholder
    if (imageView.getImage() == null) {
      Label imgLabel = new Label("No Image Available");
      imgLabel.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 14; -fx-font-style: italic;");
      imageContainer.getChildren().add(imgLabel);
    }

    // Thiết lập kích thước hiển thị cho ảnh để không làm vỡ card
    imageView.setFitWidth(310.0);
    imageView.setFitHeight(180.0);
    imageView.setPreserveRatio(true); // Giữ tỉ lệ ảnh gốc
    imageView.setSmooth(true); // Làm mượt ảnh khi scale

    imageContainer.getChildren().add(imageView);
    // --- KẾT THÚC PHẦN XỬ LÝ ẢNH ---

    VBox infoBox = new VBox();
    infoBox.setSpacing(10.0);
    infoBox.setStyle("-fx-padding: 15;");

    Label lblName = new Label(item.getItemName());
    lblName.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #333;");
    lblName.setWrapText(true); // Cho phép xuống dòng nếu tên quá dài

    Label lblPrice =
        new Label(
            "Giá hiện tại: " + String.format("%,d", auction.getHighestBid().longValue()) + " đ");
    lblPrice.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #e53935;");

    Button btnJoin = new Button("THAM GIA PHÒNG");
    btnJoin.setMaxWidth(Double.MAX_VALUE);
    btnJoin.setStyle(
        "-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");

    btnJoin.setOnAction(e -> {
      handleJoinAuction(e, auction);
    });

    infoBox.getChildren().addAll(lblName, lblPrice, btnJoin);
    card.getChildren().addAll(imageContainer, infoBox);

    return card;
  }

  private void handleJoinAuction(ActionEvent event, Auction auction) {
    try {
      // 1. Nhét dữ liệu vào Trạm trung chuyển
      AuctionSession.getInstance().setCurrentAuction(auction);
      AuctionSession.getInstance().setCurrentItem(auction.getItem());
      switchScene(event, "/views/AuctionRoomView.fxml", "Phòng đấu giá: " + auction.getItem().getItemName());

    } catch (Exception ex) {
      ex.printStackTrace();
      showAlert("Lỗi", "Không thể vào phòng đấu giá: " + ex.getMessage());
    }
  }

  @FXML
  public void handleMain(ActionEvent event) {
    switchScene(event, "/views/MainView.fxml", "Trang chủ");
  }

  @FXML
  public void handleUserUi(ActionEvent event) {
    switchScene(event, "/views/PersonalView.fxml", "Hồ sơ cá nhân");
  }

  @FXML
  public void handleLogout(ActionEvent event) {
    switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống ");
  }

  @FXML
  public void handleMenuItem(ActionEvent event) {}
}
