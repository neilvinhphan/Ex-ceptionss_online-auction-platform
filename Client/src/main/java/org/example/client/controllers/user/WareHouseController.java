package org.example.client.controllers.user;

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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import org.example.client.controllers.BaseController;
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
import org.example.core.shared.enums.ActionType;
import org.example.core.shared.enums.ItemStatus;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller phụ trách vận hành màn hình Kho hàng của tôi (My Warehouse). Cung cấp bảng dữ liệu
 * TableView thống kê tài sản cá nhân kèm các nghiệp vụ chỉnh sửa thông tin đa hình, xóa bỏ các mặt
 * hàng chưa đưa lên sảnh đấu giá (DRAFT/APPROVED/PENDING) thông qua cổng Socket giao tiếp máy chủ.
 */
public class WareHouseController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(WareHouseController.class.getName());

  @FXML private TableView<Item> productTable;
  @FXML private TableColumn<Item, String> colName;
  @FXML private TableColumn<Item, String> colDescription;
  @FXML private TableColumn<Item, BigDecimal> colStartingPrice;
  @FXML private TableColumn<Item, String> colStatus;

  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();
  private ObservableList<Item> observableItemList;

  /**
   * Phương thức thiết lập cấu trúc bảng hiển thị dữ liệu và chạy tiến trình nạp danh sách vật phẩm
   * từ DB.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    setupTableColumns();
    loadWareHouseItems();
  }

  /** Sự kiện nút bấm chuyển hướng giao diện nhanh sang form thêm mới tệp hồ sơ sản phẩm đấu giá. */
  @FXML
  public void handleAddProduct(ActionEvent event) {
    UserSidebarController.currentView = "CreateItemView.fxml";
    switchScene(event, "/views/CreateItemView.fxml", "Thêm sản phẩm đấu giá");
  }

  /**
   * Thực hiện kiểm tra ràng buộc nghiệp vụ trạng thái tài sản và gửi yêu cầu Socket xóa bỏ bản ghi
   * khỏi hệ thống.
   */
  @FXML
  public void handleDeleteProduct(ActionEvent event) {
    Item selectedItem = productTable.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      showAlert("Thông báo", "Vui lòng click chọn một sản phẩm trên bảng để xóa!");
      return;
    }

    if (selectedItem.getStatus() != null && selectedItem.getStatus() != ItemStatus.APPROVED && selectedItem.getStatus() != ItemStatus.PENDING) {
      showAlert("Cảnh báo", "Bạn chỉ có thể xóa tài sản đang ở trạng thái APPROVED hoặc PENDING (Chưa đấu giá)!");
      return;
    }

    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Xác nhận xóa");
    confirmAlert.setHeaderText("Bạn có chắc chắn muốn xóa: " + selectedItem.getItemName() + "?");

    confirmAlert.showAndWait().ifPresent(response -> {
      if (response == ButtonType.OK) {
        DeleteRequestDTO payload = new DeleteRequestDTO(selectedItem.getItemId());
        Request request = new Request(ActionType.DELETE_ITEM, payload);

        new Thread(() -> {
          try {
            String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
            Response serverResponse = gson.fromJson(jsonResponse, Response.class);

            Platform.runLater(() -> {
              if ("SUCCESS".equals(serverResponse.getStatus())) {
                observableItemList.remove(selectedItem);
                showAlert("Thành công", "Đã xóa sản phẩm khỏi cơ sở dữ liệu!");
              } else {
                int code = serverResponse.getData() instanceof Number ? ((Number) serverResponse.getData()).intValue() : -1;
                String title = (code == 4090) ? "Xung đột dữ liệu (409)" : "Lỗi khi xóa (" + code + ")";
                showAlert(title, serverResponse.getMessage());
              }
            });
          } catch (Exception e) {
            Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
          }
        }).start();
      }
    });
  }

  /**
   * Xử lý hiển thị lưới lưới GridPane Dialog điền thông tin cập nhật giá sàn, tên gọi, mô tả và
   * đồng bộ lên dữ liệu máy chủ.
   */
  @FXML
  public void handleEditProduct(ActionEvent event) {
    Item selectedItem = productTable.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      showAlert("Thông báo", "Vui lòng chọn một sản phẩm để sửa!");
      return;
    }

    int itemId = selectedItem.getItemId();
    String itemType = selectedItem.getType();

    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Chỉnh sửa sản phẩm");
    dialog.setHeaderText("Cập nhật thông tin cho: " + selectedItem.getItemName());
    ButtonType saveButtonType = new ButtonType("Lưu thay đổi", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

    TextField tfName = new TextField(selectedItem.getItemName());
    TextField tfDesc = new TextField(selectedItem.getDescription());
    TextField tfPrice = new TextField(selectedItem.getStartingPrice().toString());

    GridPane grid = new GridPane();
    grid.setHgap(10); grid.setVgap(10);
    grid.add(new Label("Tên sản phẩm:"), 0, 0); grid.add(tfName, 1, 0);
    grid.add(new Label("Mô tả:"), 0, 1); grid.add(tfDesc, 1, 1);
    grid.add(new Label("Giá khởi điểm:"), 0, 2); grid.add(tfPrice, 1, 2);
    dialog.getDialogPane().setContent(grid);

    dialog.showAndWait().ifPresent(result -> {
      if (result == saveButtonType) {
        try {
          String newName = tfName.getText().trim();
          String newDesc = tfDesc.getText().trim();
          BigDecimal newPrice = new BigDecimal(tfPrice.getText().trim());

          EditProductRequestDTO payload = new EditProductRequestDTO(itemId, newName, newDesc, newPrice, itemType);
          Request request = new Request(ActionType.UPDATE_ITEM_FULL, payload);

          new Thread(() -> {
            try {
              String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
              Response serverResponse = gson.fromJson(jsonResponse, Response.class);

              Platform.runLater(() -> {
                if ("SUCCESS".equals(serverResponse.getStatus())) {
                  showAlert("Thành công", "Đã cập nhật toàn bộ thông tin!");
                  selectedItem.setItemName(newName);
                  selectedItem.setDescription(newDesc);
                  selectedItem.setStartingPrice(newPrice);
                  productTable.refresh();
                  int index = observableItemList.indexOf(selectedItem);
                  observableItemList.set(index, selectedItem);
                } else {
                  int code = serverResponse.getData() instanceof Number ? ((Number) serverResponse.getData()).intValue() : -1;
                  String title = (code == 4090) ? "Xung đột dữ liệu (409)" : "Lỗi cập nhật (" + code + ")";
                  showAlert(title, serverResponse.getMessage());
                }
              });
            } catch (Exception e) {
              Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
            }
          }).start();
        } catch (NumberFormatException e) {
          showAlert("Lỗi", "Giá khởi điểm phải là một con số hợp lệ!");
        }
      }
    });
  }

  /**
   * Ánh xạ thuộc tính mô hình thực thể Item vào TableView và cài đặt nhà máy cellFactory kết xuất
   * văn bản enum chuỗi trạng thái.
   */
  private void setupTableColumns() {
    colName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
    colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    colStartingPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

    colStatus.setCellValueFactory(
        cellData -> {
          ItemStatus statusEnum = cellData.getValue().getStatus();
          return new SimpleStringProperty(statusEnum != null ? statusEnum.name() : "Chưa có");
        });

    observableItemList = FXCollections.observableArrayList();
    productTable.setItems(observableItemList);
  }

  /**
   * Tạo đa luồng ngầm gửi yêu cầu GET_PENDING_ITEMS lên server để lấy toàn bộ danh sách sản phẩm
   * thuộc quyền quản lý của Seller này.
   */
  private void loadWareHouseItems() {
    User currentUser = UserSession.getInstance().getCurrentUser();
    if (currentUser == null) {
      showAlert("Lỗi", "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn!");
      return;
    }
    int sellerId = currentUser.getUserId();
    PendingItemsDTO requestPayload = new PendingItemsDTO(sellerId);
    Request request = new Request(ActionType.GET_PENDING_ITEMS, requestPayload);
    String jsonRequest = gson.toJson(request);

    new Thread(
            () -> {
              try {
                logger.log(
                    Level.INFO,
                    "Đang gửi yêu cầu xin dữ liệu Kho hàng qua Socket cho user ID: {0}",
                    sellerId);
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);

                Platform.runLater(
                    () -> {
                      if ("SUCCESS".equals(response.getStatus())) {
                        String jsonData = gson.toJson(response.getData());
                        JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
                        List<Item> fetchedItems = new ArrayList<>();

                        for (JsonElement element : jsonArray) {
                          JsonObject itemObj = element.getAsJsonObject();
                          String type = itemObj.get("type").getAsString();
                          Item parsedItem = null;

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

                        observableItemList.setAll(fetchedItems);
                        logger.log(
                            Level.INFO,
                            "Đã tải xong {0} món đồ vào mô hình hiển thị Kho hàng.",
                            fetchedItems.size());
                      } else {
                        showAlert("Lỗi tải Kho hàng", response.getMessage());
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(
                    () -> showAlert("Lỗi kết nối", "Không thể lấy dữ liệu: " + e.getMessage()));
                logger.log(
                    Level.SEVERE, "Ngoại lệ luồng kết nối nạp danh sách kho hàng tài sản", e);
              }
            })
        .start();
  }
}
