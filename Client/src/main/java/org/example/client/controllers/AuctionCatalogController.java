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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
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
  // Khai báo List lưu dữ liệu gốc từ Server
  private List<Auction> allAuctionsList = new ArrayList<>();

  @FXML private CheckBox cbTypeAll, cbTypeElectronics, cbTypeVehicle, cbTypeArt;
  @FXML private RadioButton rbPriceAll, rbPrice1, rbPrice2, rbPrice3, rbPrice4, rbPrice5;
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
  // Hàm này chuyên dùng để vẽ giao diện từ 1 danh sách cho trước
  private void displayAuctions(List<Auction> auctionsToDisplay) {
    auctionFlowPane.getChildren().clear(); // Xóa sạch cái cũ

    for (Auction auction : auctionsToDisplay) {
      VBox card = createAuctionCard(auction);
      auctionFlowPane.getChildren().add(card);

      // Xử lý ảnh (Giữ nguyên code cũ của bạn)
      String base64 = auction.getItem().getImage();
      if (base64 != null && !base64.isEmpty()) {
        new Thread(() -> {
          Image img = ImageUtils.decodeBase64ToImage(base64);
          Platform.runLater(() -> {
            ImageView iv = (ImageView) card.lookup(".auction-image");
            if (iv != null && img != null) iv.setImage(img);
          });
        }).start();
      }
    }
  }
  private void loadActiveAuctions() {
    Request request = new Request("GET_ACTIVE_AUCTIONS", null);
    String jsonRequest = gson.toJson(request);
    new Thread(() -> {
      try {
        System.out.println("Đang xin dữ liệu Các phòng đấu giá...");
        String jsonResponse = clientSocket.sendRequest(jsonRequest);
        Response response = gson.fromJson(jsonResponse, Response.class);

        if ("SUCCESS".equals(response.getStatus())) {
          // --- BƯỚC 1: PARSE DỮ LIỆU NGAY TẠI LUỒNG MẠNG ---
          String jsonData = gson.toJson(response.getData());
          JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
          List<Auction> fetchedAuctions = new ArrayList<>();

          for (JsonElement element : jsonArray) {
            JsonObject auctionObj = element.getAsJsonObject();
            Auction auction = gson.fromJson(auctionObj, Auction.class);

            if (auctionObj.has("item") && !auctionObj.get("item").isJsonNull()) {
              JsonObject itemObj = auctionObj.getAsJsonObject("item");
              String type = itemObj.get("type").getAsString();
              Item parsedItem = switch (type.toUpperCase()) {
                case "ART" -> gson.fromJson(itemObj, ArtItem.class);
                case "ELECTRONICS" -> gson.fromJson(itemObj, ElectronicsItem.class);
                case "VEHICLE" -> gson.fromJson(itemObj, VehicleItem.class);
                default -> null;
              };
              auction.setItem(parsedItem);
            }
            if (auction.getItem() != null) fetchedAuctions.add(auction);
          }
          allAuctionsList = new ArrayList<>(fetchedAuctions);
          // --- BƯỚC 2: CHỈ ĐẨY VIỆC VẼ KHUNG VÀO UI ---
          Platform.runLater(() -> {
            displayAuctions(allAuctionsList);
          });
        }
      } catch (Exception e) {
        Platform.runLater(() -> showAlert("Lỗi", "Mất kết nối: " + e.getMessage()));
        e.printStackTrace();
      }
    }).start();


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
    StackPane imageContainer = new javafx.scene.layout.StackPane();
    imageContainer.setPrefHeight(180.0);
    imageContainer.setStyle("-fx-background-color: #ECEFF1; -fx-background-radius: 10 10 0 0;");

    ImageView imageView = new ImageView();
    imageView.getStyleClass().add("auction-image");
    // Kiểm tra và lấy ảnh từ Base64
//    if (item.getImage() != null && !item.getImage().isEmpty()) {
//      Image decodedImage = ImageUtils.decodeBase64ToImage(item.getImage());
//      if (decodedImage != null) {
//        imageView.setImage(decodedImage);
//      }
//    }

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
      System.out.println(auction.getBidderId());
      System.out.println(auction.getAuctionId());
      System.out.println(auction.getItem().getItemName());
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
  public void handleMenuItem(ActionEvent event) {
  }

  @FXML
  public void handleFilter(ActionEvent event) {
    // Nếu danh sách gốc chưa load xong thì không làm gì cả
    if (allAuctionsList == null || allAuctionsList.isEmpty()) return;

    List<Auction> filteredList = new ArrayList<>();

    for (Auction auction : allAuctionsList) {
      Item item = auction.getItem();
      if (item == null) continue;

      // 1. LỌC THEO LOẠI TÀI SẢN
      boolean matchType = false;
      if (cbTypeAll.isSelected()) {
        matchType = true; // Nếu chọn "Tất cả" thì loại nào cũng qua
      } else {
        // Dùng instanceof để kiểm tra Class của Item
        if (cbTypeElectronics.isSelected() && item instanceof ElectronicsItem) matchType = true;
        if (cbTypeVehicle.isSelected() && item instanceof VehicleItem) matchType = true;
        if (cbTypeArt.isSelected() && item instanceof ArtItem) matchType = true;
      }

      // Nếu không khớp loại tài sản thì bỏ qua, xét phòng tiếp theo
      if (!matchType) continue;

      // 2. LỌC THEO GIÁ HIỆN TẠI
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

      // Nếu không khớp giá thì bỏ qua
      if (!matchPrice) continue;

      // Nếu vượt qua cả 2 bài test thì cho vào danh sách hiển thị
      filteredList.add(auction);
    }
    // Sau khi lọc xong, đẩy cái list mới vào hàm vẽ UI
    displayAuctions(filteredList);
  }
}
