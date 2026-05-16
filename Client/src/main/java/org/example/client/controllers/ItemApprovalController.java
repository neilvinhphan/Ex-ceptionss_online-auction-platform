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
import org.example.core.dto.admin.AdminProcessItemDTO;
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
  @FXML private TableView<Item> itemTable;
  @FXML private TableColumn<Item, Integer> colId;
  @FXML private TableColumn<Item, String> colItemName;
  @FXML private TableColumn<Item, String> colType;
  @FXML private TableColumn<Item, String> colPrice;

  @FXML private ImageView itemImageView;
  @FXML private Label lblNoImage, lblName, lblType, lblPrice;
  @FXML private TextArea txtDescription;
  @FXML private Button btnApprove, btnReject;

  private ObservableList<Item> pendingItemsList = FXCollections.observableArrayList();
  private Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    colId.setCellValueFactory(new PropertyValueFactory<>("itemId"));
    colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
    colType.setCellValueFactory(new PropertyValueFactory<>("type"));

    colPrice.setCellValueFactory(
        cellData -> {
          if (cellData.getValue().getStartingPrice() != null) {
            return new SimpleStringProperty(
                String.format("%,.0f đ", cellData.getValue().getStartingPrice().doubleValue()));
          }
          return new SimpleStringProperty("0 đ");
        });

    itemTable.setItems(pendingItemsList);
    setupTableSelectionListener();
    loadPendingItems(); // Tải dữ liệu khi mở màn hình
  }

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

                // --- HIỂN THỊ CẢNH BÁO TỪ AI ---
                StringBuilder fullDesc = new StringBuilder();
                if (newSelection.getAiReason() != null && !newSelection.getAiReason().isEmpty()) {
                  fullDesc
                      .append("🚩 AI CẢNH BÁO: ")
                      .append(newSelection.getAiReason())
                      .append("\n");
                  fullDesc.append("--------------------------------\n");
                }

                String desc = newSelection.getDescription();
                fullDesc.append(
                    (desc != null && !desc.trim().isEmpty()) ? desc : "Không có mô tả chi tiết.");
                txtDescription.setText(fullDesc.toString());

                // Load ảnh
                updatePreviewImage(newSelection.getImage());

                btnApprove.setDisable(false);
                btnReject.setDisable(false);
              } else {
                clearDetailsPane();
              }
            });
  }

  /** TẢI DANH SÁCH PENDING TỪ SERVER */
  private void loadPendingItems() {
    User currentUser = UserSession.getInstance().getCurrentUser();
    if (currentUser == null) return;

    // Gửi ID Admin để Server xác thực quyền
    Request request = new Request("ADMIN_GET_ALL_PENDING_ITEMS", currentUser.getUserId());

    new Thread(
            () -> {
              try {
                String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
                Response response = gson.fromJson(jsonResponse, Response.class);

                if ("SUCCESS".equals(response.getStatus())) {
                  // Xử lý đa hình (Polymorphism) cho danh sách Item
                  JsonArray jsonArray =
                      JsonParser.parseString(gson.toJson(response.getData())).getAsJsonArray();
                  List<Item> fetchedItems = new ArrayList<>();

                  for (JsonElement element : jsonArray) {
                    JsonObject obj = element.getAsJsonObject();
                    String type = obj.get("type").getAsString();
                    Item item =
                        switch (type.toUpperCase()) {
                          case "ART" -> gson.fromJson(obj, ArtItem.class);
                          case "ELECTRONICS" -> gson.fromJson(obj, ElectronicsItem.class);
                          case "VEHICLE" -> gson.fromJson(obj, VehicleItem.class);
                          default -> null;
                        };
                    if (item != null) fetchedItems.add(item);
                  }

                  Platform.runLater(
                      () -> {
                        pendingItemsList.setAll(fetchedItems);
                        clearDetailsPane();
                      });
                }
              } catch (Exception e) {
                Platform.runLater(
                    () -> showAlert("Lỗi", "Không thể tải danh sách: " + e.getMessage()));
              }
            })
        .start();
  }

  private void updatePreviewImage(String base64Image) {
    if (base64Image != null && !base64Image.isEmpty()) {
      new Thread(
              () -> {
                Image img = ImageUtils.decodeBase64ToImage(base64Image);
                Platform.runLater(
                    () -> {
                      itemImageView.setImage(img);
                      lblNoImage.setVisible(img == null);
                    });
              })
          .start();
    } else {
      itemImageView.setImage(null);
      lblNoImage.setVisible(true);
    }
  }

  @FXML
  public void handleApprove(ActionEvent event) {
    processItemAction(true, "phê duyệt");
  }

  @FXML
  public void handleReject(ActionEvent event) {
    processItemAction(false, "từ chối");
  }

  private void processItemAction(boolean isApproved, String actionName) {
    Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
    if (selectedItem == null) return;

    User currentUser = UserSession.getInstance().getCurrentUser();

    Alert confirm =
        new Alert(
            Alert.AlertType.CONFIRMATION,
            "Bạn có chắc chắn muốn " + actionName + " sản phẩm này?",
            ButtonType.YES,
            ButtonType.NO);

    confirm
        .showAndWait()
        .ifPresent(
            responseBtn -> {
              if (responseBtn == ButtonType.YES) {
                AdminProcessItemDTO payload =
                    new AdminProcessItemDTO(
                        currentUser.getUserId(), isApproved, selectedItem.getItemId());
                Request request = new Request("ADMIN_PROCESS_ITEM", payload);

                new Thread(
                        () -> {
                          try {
                            String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
                            Response response = gson.fromJson(jsonResponse, Response.class);

                            Platform.runLater(
                                () -> {
                                  if ("SUCCESS".equals(response.getStatus())) {
                                    showAlert("Thành công", "Thao tác thành công!");
                                    pendingItemsList.remove(selectedItem);
                                    clearDetailsPane();
                                  } else {
                                    showAlert("Lỗi", response.getMessage());
                                  }
                                });
                          } catch (Exception e) {
                            Platform.runLater(() -> showAlert("Lỗi mạng", e.getMessage()));
                          }
                        })
                    .start();
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
