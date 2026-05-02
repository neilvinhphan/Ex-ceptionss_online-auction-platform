package org.example.client.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.DeleteRequestDTO;
import org.example.core.dto.PendingRequestDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;
import org.example.core.models.users.User;
import org.example.core.shared.enums.ItemStatus;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class WareHouseController extends BaseController implements Initializable {

    // ===== KHAI BÁO CÁC THÀNH PHẦN GIAO DIỆN =====
    @FXML private TableView<Item> productTable;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, String> colDescription;
    @FXML private TableColumn<Item, BigDecimal> colStartingPrice;
    @FXML private TableColumn<Item, String> colStatus;
    @FXML
    private MenuButton menuUser;
    // Công cụ gọi Server
    private Gson gson = ClientManager.getInstance().getGson();
    private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

    // Cái danh sách (List) này giống như "xe đẩy hàng" để hứng dữ liệu từ Server về
    private ObservableList<Item> observableItemList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            menuUser.setText(currentUser.getUserName());
        }
        // 1. Việc đầu tiên khi mở màn hình là thiết lập các cột cho cái bảng
        setupTableColumns();
        // 2. Việc thứ hai là gọi điện lên Server xin hàng
        loadWareHouseItems();
    }

    /**
     * Hàm này dạy cho cái bảng biết: Cột nào thì lấy dữ liệu gì từ cục Item
     */
    private void setupTableColumns() {
        // Cột Tên: Lấy giá trị từ biến "itemName" trong class Item
        colName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        // Cột Mô tả: Lấy từ biến "description"
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        // Cột Giá: Lấy từ biến "startingPrice"
        colStartingPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        // Cột Trạng thái: Lấy từ biến "status"
        colStatus.setCellValueFactory(cellData -> {
            ItemStatus statusEnum = cellData.getValue().getStatus();
            // Nếu status bị null thì in ra "Trống", nếu có thì lấy tên Enum (ví dụ: DRAFT)
            if (statusEnum != null) {
                return new SimpleStringProperty(statusEnum.name());
            } else {
                return new SimpleStringProperty("Chưa có");
            }
        });

        // Vì chưa có dữ liệu ngay nên khởi tạo "xe đẩy" rỗng và nhét vào bảng
        observableItemList = FXCollections.observableArrayList();
        productTable.setItems(observableItemList);
    }

    /**
     * Hàm này gọi điện lên Server xin danh sách đồ
     */
    private void loadWareHouseItems() {
        // 1. Lấy ID thằng đang đăng nhập
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            showAlert("Lỗi", "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn!");
            return;
        }
        int sellerId = currentUser.getUserId();
        // 2. Đóng gói vào cái hộp (DTO)
        PendingRequestDTO requestPayload = new PendingRequestDTO(sellerId);
        Request request = new Request("GET_PENDING_ITEMS", requestPayload);
        String jsonRequest = gson.toJson(request);
        new Thread(() -> {
            try {
                System.out.println("Đang xin dữ liệu Kho hàng cho user: " + sellerId);
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {

                        String jsonData = gson.toJson(response.getData());
                        JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
                        List<Item> fetchedItems = new ArrayList<>();
                        // Bóc tách từng món hàng ra khỏi hộp JSON
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

                        // Đổ toàn bộ hàng vừa bóc tách vào "xe đẩy" -> Bảng sẽ tự động hiện lên!
                        observableItemList.setAll(fetchedItems);
                        System.out.println("Đã tải xong " + fetchedItems.size() + " món đồ vào Kho.");

                    } else {
                        showAlert("Lỗi tải Kho hàng", response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối", "Không thể lấy dữ liệu: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleMain(ActionEvent event) { switchScene(event, "/views/MainView.fxml", "Trang chủ"); }

    @FXML
    public void handleUserUi(ActionEvent event) { switchScene(event, "/views/PersonalView.fxml", "Hồ sơ cá nhân"); }

    @FXML
    public void handleCreateItem(ActionEvent event) { switchScene(event, "/views/CreateItemView.fxml", "Tạo sản phẩm đấu giá"); }

    @FXML
    public void handleLogout(ActionEvent event) { switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống "); }

    @FXML
    public void handleAddProduct(ActionEvent event) { switchScene(event, "/views/CreateItemView.fxml", "Thêm sản phẩm đấu giá"); }

    @FXML
    public void handleCreateAuction(ActionEvent event) { switchScene(event, "/views/CreateAuctionView.fxml", "Tạo cuộc đấu giá"); }

    @FXML
    public void handleMenuItem(ActionEvent event) {
        switchScene(event, "/views/AuctionCatalogView.fxml", "Danh sach phong dau gia"); }

    @FXML
    public void handleDeleteProduct(ActionEvent event) {
        // 1. Lấy món hàng đang được click chọn trên bảng
        Item selectedItem = productTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            showAlert("Thông báo", "Vui lòng click chọn một sản phẩm trên bảng để xóa!");
            return;
        }

        // 2. Kiểm tra an toàn: Chỉ cho xóa DRAFT
        if (selectedItem.getStatus() != null && selectedItem.getStatus() != ItemStatus.DRAFT) {
            showAlert("Cảnh báo", "Bạn chỉ có thể xóa tài sản đang ở trạng thái DRAFT (Chưa đấu giá)!");
            return;
        }

        // 3. Hiện hộp thoại hỏi lại cho chắc cốp
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText("Bạn có chắc chắn muốn xóa: " + selectedItem.getItemName() + "?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Đóng gói ID gửi lên Server
                DeleteRequestDTO payload = new DeleteRequestDTO(selectedItem.getItemId());

                Request request = new Request("DELETE_ITEM", payload);
                String jsonRequest = gson.toJson(request);

                new Thread(() -> {
                    try {
                        String jsonResponse = clientSocket.sendRequest(jsonRequest);
                        Response serverResponse = gson.fromJson(jsonResponse, Response.class);

                        Platform.runLater(() -> {
                            if ("SUCCESS".equals(serverResponse.getStatus())) {
                                // Server xóa DB thành công -> Xóa luôn trên giao diện cho đỡ phải tải lại
                                observableItemList.remove(selectedItem);
                                showAlert("Thành công", "Đã xóa sản phẩm khỏi cơ sở dữ liệu!");
                            } else {
                                showAlert("Lỗi khi xóa", serverResponse.getMessage());
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
                    }
                }).start();
            }
        });
    }

    @FXML
    public void handleEditProduct(ActionEvent event) {
        // 1. Lấy món hàng đang được chọn
        Item selectedItem = productTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            showAlert("Thông báo", "Vui lòng chọn một sản phẩm trên bảng để sửa!");
            return;
        }

        // 2. Mở hộp thoại cho nhập Mô tả mới (gắn sẵn mô tả cũ vào cho dễ sửa)
        TextInputDialog dialog = new TextInputDialog(selectedItem.getDescription());
        dialog.setTitle("Sửa thông tin");
        dialog.setHeaderText("Cập nhật mô tả cho: " + selectedItem.getItemName());
        dialog.setContentText("Mô tả mới:");

        // 3. Chờ người dùng bấm OK
        dialog.showAndWait().ifPresent(newDescription -> {
            if (newDescription.trim().isEmpty()) {
                showAlert("Lỗi", "Mô tả không được để trống!");
                return;
            }

            // 4. Đóng gói gửi Server
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("itemId", selectedItem.getItemId());
            payload.put("newDescription", newDescription.trim());

            Request request = new Request("UPDATE_ITEM_DESCRIPTION", payload);
            String jsonRequest = gson.toJson(request);

            new Thread(() -> {
                try {
                    String jsonResponse = clientSocket.sendRequest(jsonRequest);
                    Response serverResponse = gson.fromJson(jsonResponse, Response.class);

                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(serverResponse.getStatus())) {
                            // Cập nhật Database thành công -> Sửa luôn chữ trên bảng
                            selectedItem.setDescription(newDescription.trim());
                            productTable.refresh(); // Bắt cái bảng vẽ lại để hiện chữ mới
                            showAlert("Thành công", "Đã cập nhật mô tả thành công!");
                        } else {
                            showAlert("Lỗi cập nhật", serverResponse.getMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
                }
            }).start();
        });
    }


}