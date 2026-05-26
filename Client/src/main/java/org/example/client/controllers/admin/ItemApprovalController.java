package org.example.client.controllers.admin;

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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.example.client.controllers.BaseController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.ImageUtils;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.admin.AdminProcessItemDTO;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;
import org.example.core.models.users.User;
import org.example.core.shared.enums.ActionType;

/** Controller xử lý quy trình phê duyệt hoặc từ chối các tài sản (Item) do người dùng đăng tải. */
public class ItemApprovalController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(ItemApprovalController.class.getName());

  @FXML private TableView<Item> itemTable;
  @FXML private TableColumn<Item, Integer> colId;
  @FXML private TableColumn<Item, String> colItemName;
  @FXML private TableColumn<Item, String> colType;
  @FXML private TableColumn<Item, String> colPrice;
  @FXML private ImageView itemImageView;
  @FXML private Label lblNoImage;
  @FXML private Label lblName;
  @FXML private Label lblType;
  @FXML private Label lblPrice;
  @FXML private TextArea txtDescription;
  @FXML private Button btnApprove;
  @FXML private Button btnReject;

  private final ObservableList<Item> pendingItemsList = FXCollections.observableArrayList();
  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  /**
   * Khởi tạo bảng dữ liệu, cấu hình ánh xạ các cột và thiết lập bộ lắng nghe sự kiện chọn phần tử.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    colId.setCellValueFactory(new PropertyValueFactory<>("itemId"));
    colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
    colType.setCellValueFactory(new PropertyValueFactory<>("type"));

    colPrice.setCellValueFactory(
        cellData -> {
          if (cellData.getValue().getStartingPrice() != null) {
            String formattedPrice =
                String.format("%,.0f đ", cellData.getValue().getStartingPrice().doubleValue());
            return new SimpleStringProperty(formattedPrice);
          }
          return new SimpleStringProperty("NULL");
        });

    itemTable.setItems(pendingItemsList);
    setupTableSelectionListener();
    loadPendingItems();
  }

  /**
   * Đăng ký bộ lắng nghe sự kiện khi Admin chọn một dòng trên bảng để cập nhật khung hiển thị chi
   * tiết sản phẩm.
   */
  private void setupTableSelectionListener() {
    itemTable
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldSelection, newSelection) -> {
              if (newSelection != null) {
                lblName.setText(newSelection.getItemName());
                lblType.setText(newSelection.getType());
                lblPrice.setText(
                    String.format("%,.0f đ", newSelection.getStartingPrice().doubleValue()));

                String desc = newSelection.getDescription();
                txtDescription.setText(
                    (desc != null && !desc.trim().isEmpty()) ? desc : "Không có mô tả chi tiết.");

                String base64Image = newSelection.getImage();
                if (base64Image != null && !base64Image.isEmpty()) {
                  new Thread(
                          () -> {
                            try {
                              Image img = ImageUtils.decodeBase64ToImage(base64Image);
                              Platform.runLater(
                                  () -> {
                                    if (img != null) {
                                      itemImageView.setImage(img);
                                      lblNoImage.setVisible(false);
                                    } else {
                                      itemImageView.setImage(null);
                                      lblNoImage.setVisible(true);
                                    }
                                  });
                            } catch (Exception e) {
                              logger.log(
                                  Level.WARNING,
                                  "Không thể giải mã hình ảnh Base64 của tài sản",
                                  e);
                              Platform.runLater(
                                  () -> {
                                    itemImageView.setImage(null);
                                    lblNoImage.setVisible(true);
                                  });
                            }
                          })
                      .start();
                } else {
                  itemImageView.setImage(null);
                  lblNoImage.setVisible(true);
                }

                btnApprove.setDisable(false);
                btnReject.setDisable(false);
              } else {
                clearDetailsPane();
              }
            });
  }

  /**
   * Tạo tiến trình bất đồng bộ gửi yêu cầu lên Server để tải danh sách tất cả tài sản đang trong
   * trạng thái chờ duyệt.
   */
  private void loadPendingItems() {
    User currentUser = UserSession.getInstance().getCurrentUser();
    if (currentUser == null) {
      showAlert("Lỗi", "Không tìm thấy thông tin Admin. Vui lòng đăng nhập lại!");
      return;
    }

    int adminId = currentUser.getUserId();
    Request request = new Request(ActionType.ADMIN_GET_ALL_PENDING_ITEMS, adminId);

    new Thread(
            () -> {
              try {
                logger.info("Đang gửi yêu cầu lấy danh sách tài sản chờ duyệt từ Server...");
                String requestJson = gson.toJson(request);
                String jsonResponse = clientSocket.sendRequest(requestJson);

                if (jsonResponse != null) {
                  Response response = gson.fromJson(jsonResponse, Response.class);

                  if ("SUCCESS".equals(response.getStatus())) {
                    String jsonData = gson.toJson(response.getData());
                    JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
                    List<Item> fetchedItems = new ArrayList<>();

                    for (JsonElement element : jsonArray) {
                      JsonObject itemObj = element.getAsJsonObject();

                      if (itemObj.has("type") && !itemObj.get("type").isJsonNull()) {
                        String type = itemObj.get("type").getAsString();

                        Item parsedItem =
                            switch (type.toUpperCase()) {
                              case "ART" -> gson.fromJson(itemObj, ArtItem.class);
                              case "ELECTRONICS" -> gson.fromJson(itemObj, ElectronicsItem.class);
                              case "VEHICLE" -> gson.fromJson(itemObj, VehicleItem.class);
                              default -> gson.fromJson(itemObj, Item.class);
                            };
                        fetchedItems.add(parsedItem);
                      } else {
                        fetchedItems.add(gson.fromJson(itemObj, Item.class));
                      }
                    }

                    Platform.runLater(
                        () -> {
                          pendingItemsList.setAll(fetchedItems);
                          clearDetailsPane();
                          logger.info(
                              "Đã tải xong "
                                  + fetchedItems.size()
                                  + " tài sản chờ duyệt từ hệ thống.");
                        });

                  } else {
                    logger.warning(
                        "Server từ chối cung cấp danh sách tài sản chờ duyệt: "
                            + response.getMessage());
                    Platform.runLater(() -> showAlert("Lỗi tải dữ liệu", response.getMessage()));
                  }
                }
              } catch (Exception e) {
                logger.log(
                    Level.SEVERE,
                    "Lỗi nghiêm trọng khi lấy danh sách tài sản chờ duyệt từ mạng",
                    e);
                Platform.runLater(
                    () ->
                        showAlert(
                            "Lỗi mạng", "Không thể lấy dữ liệu. Chi tiết lỗi: " + e.getMessage()));
              }
            })
        .start();
  }

  /**
   * Xử lý sự kiện khi bấm nút phê duyệt tài sản.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  public void handleApprove(ActionEvent event) {
    processItemAction(true, "phê duyệt");
  }

  /**
   * Xử lý sự kiện khi bấm nút từ chối tài sản.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  public void handleReject(ActionEvent event) {
    processItemAction(false, "từ chối");
  }

  /**
   * Hàm dùng chung để thực hiện việc gửi trạng thái Phê duyệt hoặc Từ chối xử lý tài sản lên phía
   * Server.
   *
   * @param isApproved Trạng thái duyệt (true là phê duyệt, false là từ chối).
   * @param actionName Tên hành động hiển thị lên hộp thoại xác nhận.
   */
    private void processItemAction(boolean isApproved, String actionName) {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            showAlert("Lỗi", "Không tìm thấy phiên đăng nhập của Admin!");
            return;
        }

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Bạn có chắc chắn muốn " + actionName + " sản phẩm: " + selectedItem.getItemName() + "?",
                ButtonType.YES,
                ButtonType.NO);
        confirm.setTitle("Xác nhận thao tác");

        confirm.showAndWait().ifPresent(responseBtn -> {
            if (responseBtn == ButtonType.YES) {
                AdminProcessItemDTO payload = new AdminProcessItemDTO(
                        currentUser.getUserId(), isApproved, selectedItem.getItemId());
                Request request = new Request(ActionType.ADMIN_PROCESS_ITEM, payload);

                new Thread(() -> {
                    try {
                        String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
                        Response response = gson.fromJson(jsonResponse, Response.class);

                        Platform.runLater(() -> {
                            if ("SUCCESS".equals(response.getStatus())) {
                                showAlert("Thành công", "Đã " + actionName + " sản phẩm thành công!");
                                pendingItemsList.remove(selectedItem);
                                clearDetailsPane();
                            } else {
                                int code = response.getData() instanceof Number ? ((Number) response.getData()).intValue() : -1;
                                String errorTitle = switch (code) {
                                    case 4040 -> "Không tìm thấy (404)";
                                    case 4030 -> "Lỗi phân quyền (403)";
                                    case 4000 -> "Lỗi nghiệp vụ (400)";
                                    case 5000 -> "Lỗi máy chủ (500)";
                                    default -> "Lỗi xử lý (" + code + ")";
                                };
                                logger.warning("Thao tác thất bại [" + code + "]: " + response.getMessage());
                                showAlert(errorTitle, response.getMessage());
                            }
                        });
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Lỗi kết nối mạng khi xử lý tác vụ", e);
                        Platform.runLater(() -> showAlert("Lỗi kết nối", "Chi tiết: " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

  /**
   * Làm mới lại danh sách dữ liệu tài sản chờ phê duyệt.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  public void handleRefresh(ActionEvent event) {
    loadPendingItems();
  }

  /**
   * Chuyển hướng trở về giao diện màn hình chính tổng quan của ứng dụng.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  public void handleBack(ActionEvent event) {
    switchScene(event, "/views/MainView.fxml", "Trang chủ");
  }

  /**
   * Dọn sạch thông tin hiển thị tại khung chi tiết bên phải và đưa các nút điều hướng về trạng thái
   * vô hiệu hóa.
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
