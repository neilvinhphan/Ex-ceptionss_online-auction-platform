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
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;
import org.example.core.models.users.User;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AuctionCatalogController extends BaseController implements Initializable {

    @FXML
    private FlowPane auctionFlowPane;

    @FXML
    private MenuButton menuUser;

    private Gson gson = ClientManager.getInstance().getGson();
    private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Cập nhật tên người dùng đang đăng nhập
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            menuUser.setText(currentUser.getUserName());
        }

        // Vừa mở giao diện lên là gọi Server lấy danh sách đấu giá
        loadActiveAuctions();
    }

    /**
     * Hàm gọi lên Server lấy danh sách các cuộc đấu giá
     */
    private void loadActiveAuctions() {
        // Gửi Request không cần payload vì ta lấy TẤT CẢ các sản phẩm đang/sắp đấu
        Request request = new Request("GET_ACTIVE_AUCTIONS", null);
        String jsonRequest = gson.toJson(request);

        new Thread(() -> {
            try {
                System.out.println("Đang xin dữ liệu Các phòng đấu giá...");
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);

                Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {

                        // --- ĐOẠN NÀY LÀ BÓC TÁCH JSON Y HỆT BÊN WAREHOUSE CỦA ĐỆ ---
                        String jsonData = gson.toJson(response.getData());
                        JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
                        List<Item> fetchedItems = new ArrayList<>();

                        for (JsonElement element : jsonArray) {
                            JsonObject itemObj = element.getAsJsonObject();
                            String type = itemObj.get("type").getAsString();
                            Item parsedItem = null;

                            // Ép kiểu cho đúng loại hàng
                            switch (type.toUpperCase()) {
                                case "ART" -> parsedItem = gson.fromJson(itemObj, ArtItem.class);
                                case "ELECTRONICS" -> parsedItem = gson.fromJson(itemObj, ElectronicsItem.class);
                                case "VEHICLE" -> parsedItem = gson.fromJson(itemObj, VehicleItem.class);
                            }

                            if (parsedItem != null) {
                                fetchedItems.add(parsedItem);
                            }
                        }
                        // -----------------------------------------------------------

                        System.out.println("Đã tải xong " + fetchedItems.size() + " phòng đấu giá.");

                        // Xóa giao diện cũ
                        auctionFlowPane.getChildren().clear();

                        // Đổ List vừa bóc tách ra giao diện FlowPane
                        for (Item item : fetchedItems) {
                            // Gọi hàm vẽ cái Card
                            VBox card = createAuctionCard(item);
                            auctionFlowPane.getChildren().add(card);
                        }

                    } else {
                        showAlert("Lỗi", "Không thể tải danh sách đấu giá: " + response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối", "Mất kết nối tới máy chủ: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Hàm vẽ giao diện thẻ (Card) cho từng sản phẩm
     */
    private VBox createAuctionCard(Item item) {
        VBox card = new VBox();
        card.setPrefWidth(310.0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");

        // Phần khung ảnh (Có thể hiển thị loại của Item cho ngầu)
        AnchorPane imagePane = new AnchorPane();
        imagePane.setPrefHeight(180.0);
        imagePane.setStyle("-fx-background-color: #ECEFF1; -fx-background-radius: 10 10 0 0;");
        Label imgLabel = new Label("[" + item.getType() + "]"); // In loại Item ra hình
        imgLabel.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 18; -fx-font-weight: bold;");
        imgLabel.setLayoutX(110.0);
        imgLabel.setLayoutY(80.0);
        imagePane.getChildren().add(imgLabel);

        // Phần thông tin
        VBox infoBox = new VBox();
        infoBox.setSpacing(10.0);
        infoBox.setStyle("-fx-padding: 15;");

        Label lblName = new Label(item.getItemName());
        lblName.setStyle("-fx-font-size: 14; -fx-text-fill: #555;");

        // Format giá cho đẹp, ví dụ: 5000000 -> 5,000,000 đ
        Label lblPrice = new Label("Giá k.điểm: " + String.format("%,d", item.getStartingPrice().longValue()) + " đ");
        lblPrice.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #333;");

        Button btnJoin = new Button("THAM GIA PHÒNG");
        btnJoin.setMaxWidth(Double.MAX_VALUE);
        btnJoin.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        // Gắn sự kiện click
        btnJoin.setOnAction(e -> {
            System.out.println("Đang vào phòng đấu giá của Item ID: " + item.getItemId());
            handleJoinAuction(item);
        });

        infoBox.getChildren().addAll(lblName, lblPrice, btnJoin);
        card.getChildren().addAll(imagePane, infoBox);

        return card;
    }

    private void handleJoinAuction(Item item) {
        // Viết logic để chuyển Scene sang phòng đấu giá, truyền cái Item đó sang
        // ...
        showAlert("Thông báo", "Bạn vừa chọn tham gia phòng: " + item.getItemName());
    }

    // ==== CÁC HÀM XỬ LÝ MENU GIỮ NGUYÊN ====
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
        // Tùy logic của đệ
    }
}