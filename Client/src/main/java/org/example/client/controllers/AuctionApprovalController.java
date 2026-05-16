package org.example.client.controllers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
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
import org.example.core.models.entities.Auction;
import org.example.core.models.items.Item;
import org.example.core.models.users.User;
import org.example.core.shared.enums.AuctionStatus;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AuctionApprovalController extends BaseController implements Initializable {

  // --- CÁC THÀNH PHẦN GIAO DIỆN (Đã khớp fx:id với FXML của My) ---
  @FXML private TableView<Auction> itemTable; // Dùng TableView để chứa Auction
  @FXML private TableColumn<Auction, Integer> colId;
  @FXML private TableColumn<Auction, String> colItemName;
  @FXML private TableColumn<Auction, String> colType;
  @FXML private TableColumn<Auction, String> colPrice;

  @FXML private ImageView itemImageView;
  @FXML private Label lblNoImage, lblName, lblType, lblPrice;
  @FXML private TextArea txtDescription;
  @FXML private Button btnApprove, btnReject;

  private ObservableList<Auction> pendingAuctionList = FXCollections.observableArrayList();
  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    setupTableColumns();
    loadPendingAuctions();

    // 🎧 Lắng nghe sự kiện chọn dòng trong bảng để hiển thị Preview
    itemTable
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldSel, newSel) -> {
              if (newSel != null) {
                showAuctionPreview(newSel);
                btnApprove.setDisable(false);
                btnReject.setDisable(false);
              } else {
                clearPreview();
                btnApprove.setDisable(true);
                btnReject.setDisable(true);
              }
            });
  }

  // =======================================================
  // 1. CÀI ĐẶT BẢNG (MAP DỮ LIỆU AUCTION)
  // =======================================================
  private void setupTableColumns() {
    colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));

    // Móc tên sản phẩm từ object Item nằm trong Auction
    colItemName.setCellValueFactory(
        cellData -> {
          Item item = cellData.getValue().getItem();
          return new SimpleStringProperty(item != null ? item.getItemName() : "N/A");
        });

    colType.setCellValueFactory(
        cellData -> {
          Item item = cellData.getValue().getItem();
          return new SimpleStringProperty(item != null ? item.getType() : "N/A");
        });

    colPrice.setCellValueFactory(
        cellData -> {
          Item item = cellData.getValue().getItem();
          if (item != null && item.getStartingPrice() != null) {
            return new SimpleStringProperty(
                String.format("%,d đ", item.getStartingPrice().longValue()));
          }
          return new SimpleStringProperty("0 đ");
        });
  }

  private void showAuctionPreview(Auction auction) {
    Item item = auction.getItem();
    if (item == null) return;

    lblName.setText(item.getItemName());
    lblType.setText(item.getType());
    lblPrice.setText(String.format("%,d VND", item.getStartingPrice().longValue()));
    txtDescription.setText(item.getDescription());

    // Xử lý ảnh Base64
    if (item.getImage() != null && !item.getImage().isEmpty()) {
      lblNoImage.setVisible(false);
      new Thread(
              () -> {
                Image img = ImageUtils.decodeBase64ToImage(item.getImage());
                Platform.runLater(() -> itemImageView.setImage(img));
              })
          .start();
    } else {
      itemImageView.setImage(null);
      lblNoImage.setVisible(true);
      lblNoImage.setText("Không có ảnh");
    }
  }

  private void clearPreview() {
    lblName.setText("...");
    lblType.setText("...");
    lblPrice.setText("...");
    txtDescription.setText("");
    itemImageView.setImage(null);
    lblNoImage.setVisible(true);
  }

  // =======================================================
  // 2. GIAO TIẾP SERVER (LẤY DANH SÁCH PENDING)
  // =======================================================
  @FXML
  private void handleRefresh(ActionEvent event) {
    loadPendingAuctions();
  }

    private void loadPendingAuctions() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;
        int adminId = currentUser.getUserId();

        // Thêm adminId vào Request
        Request request = new Request("GET_PENDING_AUCTIONS", adminId);
        new Thread(
                () -> {
                    try {
                        String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
                        Response response = gson.fromJson(jsonResponse, Response.class);

                        if ("SUCCESS".equals(response.getStatus())) {
                            // BÓC TÁCH ĐA HÌNH ITEM BẰNG TAY (GIỐNG MANAGE AUCTION)
                            String jsonData = gson.toJson(response.getData());
                            JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
                            List<Auction> fetched = new java.util.ArrayList<>();

                            for (JsonElement element : jsonArray) {
                                JsonObject auctionObj = element.getAsJsonObject();
                                Auction auction = gson.fromJson(auctionObj, Auction.class);

                                if (auctionObj.has("item") && !auctionObj.get("item").isJsonNull()) {
                                    JsonObject itemObj = auctionObj.getAsJsonObject("item");
                                    String type = itemObj.get("type").getAsString();

                                    Item parsedItem = switch (type.toUpperCase()) {
                                        case "ART" -> gson.fromJson(itemObj, org.example.core.models.items.ArtItem.class);
                                        case "ELECTRONICS" -> gson.fromJson(itemObj, org.example.core.models.items.ElectronicsItem.class);
                                        case "VEHICLE" -> gson.fromJson(itemObj, org.example.core.models.items.VehicleItem.class);
                                        default -> gson.fromJson(itemObj, Item.class);
                                    };
                                    auction.setItem(parsedItem);
                                }
                                fetched.add(auction);
                            }

                            Platform.runLater(
                                    () -> {
                                        pendingAuctionList.setAll(fetched);
                                        itemTable.setItems(pendingAuctionList);
                                        clearPreview();
                                    });
                        }
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Lỗi", "Không thể tải dữ liệu: " + e.getMessage()));
                    }
                })
                .start();
    }

    @FXML
    private void handleApprove(ActionEvent event) {
        Auction selected = itemTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Thông báo", "Vui lòng chọn một phiên đấu giá để duyệt!");
            return;
        }

        int adminId = UserSession.getInstance().getCurrentUser().getUserId();

        // Sử dụng DTO chuẩn thay vì nhét tay vào JsonObject
        org.example.core.dto.admin.AdminApproveAuctionDTO approveDto =
                new org.example.core.dto.admin.AdminApproveAuctionDTO(adminId, selected.getAuctionId());

        Request request = new Request("APPROVE_AUCTION", approveDto);

        new Thread(
                () -> {
                    try {
                        String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
                        Response res = gson.fromJson(jsonResponse, Response.class);
                        Platform.runLater(
                                () -> {
                                    if ("SUCCESS".equals(res.getStatus())) {
                                        showAlert("Thành công", "Đã duyệt! Phòng sẽ tự mở vào lúc: " + selected.getStartTime());
                                        loadPendingAuctions();
                                    } else {
                                        showAlert("Lỗi", res.getMessage());
                                    }
                                });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
                    }
                })
                .start();
    }

  @FXML
  private void handleReject(ActionEvent event) {
    Auction selected = itemTable.getSelectionModel().getSelectedItem();
    if (selected == null) return;

    int adminId = UserSession.getInstance().getCurrentUser().getUserId();
    // Dùng DTO Hủy đấu giá đã sửa của anh em mình
    org.example.core.dto.admin.AdminCancelAuctionDTO cancelReq =
        new org.example.core.dto.admin.AdminCancelAuctionDTO(selected.getAuctionId(), adminId);

    Request request = new Request("ADMIN_CANCEL_AUCTION", cancelReq);

    new Thread(
            () -> {
              try {
                String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
                Response res = gson.fromJson(jsonResponse, Response.class);
                Platform.runLater(
                    () -> {
                      if ("SUCCESS".equals(res.getStatus())) {
                        showAlert("Thành công", "Đã từ chối phiên đấu giá.");
                        loadPendingAuctions();
                      } else {
                        showAlert("Lỗi", res.getMessage());
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi mạng", e.getMessage()));
              }
            })
        .start();
  }

  @FXML
  private void handleBack(ActionEvent event) {
    switchScene(event, "/views/ManageAuctionView.fxml", "Quản trị hệ thống");
  }
}
