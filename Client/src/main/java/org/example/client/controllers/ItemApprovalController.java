package org.example.client.controllers;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.ImageUtils;
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

public class ItemApprovalController extends BaseController implements Initializable {
    // --- CÁC THÀNH PHẦN BẢNG BÊN TRÁI ---
    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, Integer> colId;
    @FXML private TableColumn<Item, String> colItemName;
    @FXML private TableColumn<Item, String> colType;
    @FXML private TableColumn<Item, String> colPrice;
    // --- CÁC THÀNH PHẦN CHI TIẾT BÊN PHẢI ---
    @FXML private ImageView itemImageView;
    @FXML private Label lblNoImage;
    @FXML private Label lblName;
    @FXML private Label lblType;
    @FXML private Label lblPrice;
    @FXML private TextArea txtDescription;
    @FXML private Button btnApprove;
    @FXML private Button btnReject;
    private ObservableList<Item> pendingItemsList = FXCollections.observableArrayList();
    private Gson gson = ClientManager.getInstance().getGson();
    private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Khởi tạo các cột cho bảng
        colId.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        // Cột Giá: Format lại thành chuỗi có dấu phẩy
        colPrice.setCellValueFactory(cellData -> {
            if (cellData.getValue().getStartingPrice() != null) {
                String formattedPrice = String.format("%,.0f đ", cellData.getValue().getStartingPrice().doubleValue());
                return new SimpleStringProperty(formattedPrice);
            }
            return new SimpleStringProperty("NULL");
        });
        // 2. Gắn list vào bảng
        itemTable.setItems(pendingItemsList);
        // 3. Lắng nghe sự kiện click vào 1 dòng trên bảng
        setupTableSelectionListener();
        // 4. Gọi API tải dữ liệu lần đầu
        loadPendingItems();
    }

    private void setupTableSelectionListener() {
        itemTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Cập nhật text
                lblName.setText(newSelection.getItemName());
                lblType.setText(newSelection.getType());
                lblPrice.setText(String.format("%,.0f đ", newSelection.getStartingPrice().doubleValue()));

                // Nếu mô tả trống, hiển thị chữ mặc định
                String desc = newSelection.getDescription();
                txtDescription.setText((desc != null && !desc.trim().isEmpty()) ? desc : "Không có mô tả chi tiết.");

                // Cập nhật Ảnh
                String base64Image = newSelection.getImage();
                if (base64Image != null && !base64Image.isEmpty()) {
                    new Thread(() -> {
                        Image img = ImageUtils.decodeBase64ToImage(base64Image);
                        Platform.runLater(() -> {
                            if (img != null) {
                                itemImageView.setImage(img);
                                lblNoImage.setVisible(false);
                            } else {
                                itemImageView.setImage(null);
                                lblNoImage.setVisible(true);
                            }
                        });
                    }).start();
                } else {
                    itemImageView.setImage(null);
                    lblNoImage.setVisible(true);
                }

                // Mở khóa các nút
                btnApprove.setDisable(false);
                btnReject.setDisable(false);
            } else {
                clearDetailsPane();
            }
        });
    }

    private void loadPendingItems() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Reset giao diện trước khi tải mới
        clearDetailsPane();

        // Request: Lấy danh sách sản phẩm đang ở trạng thái PENDING
        Request request = new Request("GET_PENDING_ITEMS", null);
        String jsonRequest = gson.toJson(request);

        new Thread(() -> {
            try {
                System.out.println("Đang lấy danh sách hàng chờ duyệt từ Server...");
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);

                if ("SUCCESS".equals(response.getStatus())) {
                    // Bóc tách JSON Đa hình (Tương tự file Auction)
                    String jsonData = gson.toJson(response.getData());
                    JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
                    List<Item> fetchedItems = new ArrayList<>();

                    for (JsonElement element : jsonArray) {
                        JsonObject itemObj = element.getAsJsonObject();
                        String type = itemObj.has("type") && !itemObj.get("type").isJsonNull()
                                ? itemObj.get("type").getAsString() : "";

                        Item parsedItem = switch (type.toUpperCase()) {
                            case "ART" -> gson.fromJson(itemObj, ArtItem.class);
                            case "ELECTRONICS" -> gson.fromJson(itemObj, ElectronicsItem.class);
                            case "VEHICLE" -> gson.fromJson(itemObj, VehicleItem.class);
                            default -> gson.fromJson(itemObj, Item.class); // Fallback
                        };
                        fetchedItems.add(parsedItem);
                    }

                    Platform.runLater(() -> {
                        pendingItemsList.setAll(fetchedItems);
                    });
                } else {
                    Platform.runLater(() -> showAlert( "Lỗi tải dữ liệu", response.getMessage()));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleApprove(ActionEvent event) {
        processItemAction("APPROVE_ITEM", "phê duyệt");
    }

    @FXML
    public void handleReject(ActionEvent event) {
        processItemAction("REJECT_ITEM", "từ chối");
    }

    /**
     * Hàm chung để xử lý việc Duyệt hoặc Từ chối sản phẩm
     */
    private void processItemAction(String actionCommand, String actionName) {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;

        // Hiển thị hộp thoại xác nhận cho Admin
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc chắn muốn " + actionName + " sản phẩm: " + selectedItem.getItemName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận thao tác");

        confirm.showAndWait().ifPresent(responseBtn -> {
            if (responseBtn == ButtonType.YES) {
                // Tạo Request gửi ID của Item lên server
                Request request = new Request(actionCommand, selectedItem.getItemId());
                String jsonRequest = gson.toJson(request);

                new Thread(() -> {
                    try {
                        String jsonResponse = clientSocket.sendRequest(jsonRequest);
                        Response response = gson.fromJson(jsonResponse, Response.class);

                        Platform.runLater(() -> {
                            if ("SUCCESS".equals(response.getStatus())) {
                                showAlert( "Thành công", "Đã " + actionName + " sản phẩm thành công!");
                                // Xóa khỏi danh sách chờ trên giao diện
                                pendingItemsList.remove(selectedItem);
                                clearDetailsPane();
                            } else {
                                showAlert( "Lỗi", response.getMessage());
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        loadPendingItems();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    /**
     * Xóa sạch thông tin ở khung bên phải khi không có dòng nào được chọn
     */
    private void clearDetailsPane() {
        lblName.setText("...");
        lblType.setText("...");
        lblPrice.setText("...");
        txtDescription.setText("");
        itemImageView.setImage(null);
        lblNoImage.setVisible(true);
        btnApprove.setDisable(true);
        btnReject.setDisable(true);
    }
}