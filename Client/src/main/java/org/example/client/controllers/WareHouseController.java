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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.itemsDTO.DeleteRequestDTO;
import org.example.core.dto.itemsDTO.EditProductRequestDTO;
import org.example.core.dto.itemsDTO.PendingItemsDTO;
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
  // Công cụ gọi Server
  private Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  // Cái danh sách (List) này giống như "xe đẩy hàng" để hứng dữ liệu từ Server về
  private ObservableList<Item> observableItemList;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    User currentUser = UserSession.getInstance().getCurrentUser();
    // 1. Việc đầu tiên khi mở màn hình là thiết lập các cột cho cái bảng
    setupTableColumns();
    // 2. Việc thứ hai là gọi điện lên Server xin hàng
    loadWareHouseItems();
  }

  /** Hàm này dạy cho cái bảng biết: Cột nào thì lấy dữ liệu gì từ cục Item */
  private void setupTableColumns() {
    // Cột Tên: Lấy giá trị từ biến "itemName" trong class Item
    colName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
    // Cột Mô tả: Lấy từ biến "description"
    colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    // Cột Giá: Lấy từ biến "startingPrice"
    colStartingPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
    // Cột Trạng thái: Lấy từ biến "status"
    colStatus.setCellValueFactory(
        cellData -> {
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

  /** Hàm này gọi điện lên Server xin danh sách đồ */
  private void loadWareHouseItems() {
    // 1. Lấy ID thằng đang đăng nhập
    User currentUser = UserSession.getInstance().getCurrentUser();
    if (currentUser == null) {
      showAlert("Lỗi", "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn!");
      return;
    }
    int sellerId = currentUser.getUserId();
    // 2. Đóng gói vào cái hộp (DTO)
    PendingItemsDTO requestPayload = new PendingItemsDTO(sellerId);
    Request request = new Request("GET_PENDING_ITEMS", requestPayload);
    String jsonRequest = gson.toJson(request);
    new Thread(
            () -> {
              try {
                System.out.println("Đang xin dữ liệu Kho hàng cho user: " + sellerId);
                String jsonResponse = clientSocket.sendRequest(jsonRequest);

                System.out.println("DEBUG Dữ liệu gốc từ Server: " + jsonResponse); // Thêm dòng này

                Response response = gson.fromJson(jsonResponse, Response.class);
                Platform.runLater(
                    () -> {
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
                            case "ELECTRONICS" ->
                                parsedItem = gson.fromJson(itemObj, ElectronicsItem.class);
                            case "VEHICLE" ->
                                parsedItem = gson.fromJson(itemObj, VehicleItem.class);
                          }

                          if (parsedItem != null) {
                            fetchedItems.add(parsedItem);
                          }
                        }

                        // Đổ toàn bộ hàng vừa bóc tách vào "xe đẩy" -> Bảng sẽ tự động hiện lên!
                        observableItemList.setAll(fetchedItems);
                        System.out.println(
                            "Đã tải xong " + fetchedItems.size() + " món đồ vào Kho.");

                      } else {
                        showAlert("Lỗi tải Kho hàng", response.getMessage());
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(
                    () -> showAlert("Lỗi kết nối", "Không thể lấy dữ liệu: " + e.getMessage()));
                e.printStackTrace();
              }
            })
        .start();
  }

  @FXML
  public void handleAddProduct(ActionEvent event) {
    UserSidebarController.currentView = "CreateItemView.fxml";
    switchScene(event, "/views/CreateItemView.fxml", "Thêm sản phẩm đấu giá");
  }


  @FXML
  public void handleDeleteProduct(ActionEvent event) {
    // 1. Lấy món hàng đang được click chọn trên bảng
    Item selectedItem = productTable.getSelectionModel().getSelectedItem();

    if (selectedItem == null) {
      showAlert("Thông báo", "Vui lòng click chọn một sản phẩm trên bảng để xóa!");
      return;
    }

    // 2. Kiểm tra an toàn: Chỉ cho xóa DRAFT
    if (selectedItem.getStatus() != null && selectedItem.getStatus() != ItemStatus.APPROVED && selectedItem.getStatus() != ItemStatus.PENDING) {
      showAlert("Cảnh báo", "Bạn chỉ có thể xóa tài sản đang ở trạng thái APPROVED hoặc PENDING (Chưa đấu giá)!");
      return;
    }

    // 3. Hiện hộp thoại hỏi lại cho chắc cốp
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Xác nhận xóa");
    confirmAlert.setHeaderText("Bạn có chắc chắn muốn xóa: " + selectedItem.getItemName() + "?");

    confirmAlert
        .showAndWait()
        .ifPresent(
            response -> {
              if (response == ButtonType.OK) {
                // Đóng gói ID gửi lên Server
                DeleteRequestDTO payload = new DeleteRequestDTO(selectedItem.getItemId());

                Request request = new Request("DELETE_ITEM", payload);
                String jsonRequest = gson.toJson(request);

                new Thread(
                        () -> {
                          try {
                            String jsonResponse = clientSocket.sendRequest(jsonRequest);
                            Response serverResponse = gson.fromJson(jsonResponse, Response.class);

                            Platform.runLater(
                                () -> {
                                  if ("SUCCESS".equals(serverResponse.getStatus())) {
                                    // Server xóa DB thành công -> Xóa luôn trên giao diện cho đỡ
                                    // phải tải lại
                                    observableItemList.remove(selectedItem);
                                    showAlert("Thành công", "Đã xóa sản phẩm khỏi cơ sở dữ liệu!");
                                  } else {
                                    showAlert("Lỗi khi xóa", serverResponse.getMessage());
                                  }
                                });
                          } catch (Exception e) {
                            Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
                          }
                        })
                    .start();
              }
            });
  }

  @FXML
  public void handleEditProduct(ActionEvent event) {
    // 1. Lấy dòng đang chọn
    Item selectedItem = productTable.getSelectionModel().getSelectedItem();

    if (selectedItem == null) {
      showAlert("Thông báo", "Vui lòng chọn một sản phẩm để sửa!");
      return;
    }
    int itemId = selectedItem.getItemId();
    String itemType = selectedItem.getType();

    // 1. Tạo một Dialog tùy chỉnh
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Chỉnh sửa sản phẩm");
    dialog.setHeaderText("Cập nhật thông tin cho: " + selectedItem.getItemName());

    // 2. Thiết lập các nút bấm (Lưu và Hủy)
    ButtonType saveButtonType = new ButtonType("Lưu thay đổi", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

    // 3. Tạo các ô nhập liệu (TextField)
    TextField tfName = new TextField(selectedItem.getItemName());
    TextField tfDesc = new TextField(selectedItem.getDescription());
    TextField tfPrice = new TextField(selectedItem.getStartingPrice().toString());

    // 4. Sắp xếp các ô vào một cái lưới (GridPane) cho đẹp
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.add(new Label("Tên sản phẩm:"), 0, 0);
    grid.add(tfName, 1, 0);
    grid.add(new Label("Mô tả:"), 0, 1);
    grid.add(tfDesc, 1, 1);
    grid.add(new Label("Giá khởi điểm:"), 0, 2);
    grid.add(tfPrice, 1, 2);

    dialog.getDialogPane().setContent(grid);

    // 5. Chờ người dùng bấm nút
    dialog
        .showAndWait()
        .ifPresent(
            result -> {
              if (result == saveButtonType) {
                try {
                  String newName = tfName.getText().trim();
                  String newDesc = tfDesc.getText().trim();
                  BigDecimal newPrice = new BigDecimal(tfPrice.getText().trim());

                  // 6. Đóng gói "full combo" gửi lên Server
                  EditProductRequestDTO payload =
                      new EditProductRequestDTO(itemId, newName, newDesc, newPrice, itemType);

                  Request request = new Request("UPDATE_ITEM_FULL", payload); // Đổi Action cho kêu
                  String jsonRequest = gson.toJson(request);

                  new Thread(
                          () -> {
                            try {
                              String jsonResponse = clientSocket.sendRequest(jsonRequest);
                              Response serverResponse = gson.fromJson(jsonResponse, Response.class);

                              Platform.runLater(() -> {
                                if ("SUCCESS".equals(serverResponse.getStatus())) {
                                  showAlert("Thành công", "Đã cập nhật toàn bộ thông tin!");

                                  // 1. Cập nhật dữ liệu vào Object (Bạn đã làm rồi)
                                  selectedItem.setItemName(newName);
                                  selectedItem.setDescription(newDesc);
                                  selectedItem.setStartingPrice(newPrice);

                                  // 2. QUAN TRỌNG: Làm mới bảng để hiển thị giá trị mới
                                  productTable.refresh();

                                  // 3. (Tùy chọn) Để chắc chắn hơn, bạn có thể thay thế chính nó trong list:
                                  int index = observableItemList.indexOf(selectedItem);
                                  observableItemList.set(index, selectedItem);

                                } else {
                                  showAlert("Lỗi", serverResponse.getMessage());
                                }
                              });
                            } catch (Exception e) {
                              Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
                            }
                          })
                      .start();

                } catch (NumberFormatException e) {
                  showAlert("Lỗi", "Giá khởi điểm phải là một con số hợp lệ!");
                }
              }
            });
  }

}
