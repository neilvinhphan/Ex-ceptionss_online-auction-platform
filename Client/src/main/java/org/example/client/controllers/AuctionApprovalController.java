package org.example.client.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
    Request request = new Request("GET_PENDING_AUCTIONS", null);
    new Thread(
            () -> {
              try {
                String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
                System.out.println("DEBUG SERVER TRẢ VỀ: " + jsonResponse);
                Response response = gson.fromJson(jsonResponse, Response.class);

                if ("SUCCESS".equals(response.getStatus())) {
                  String jsonData = gson.toJson(response.getData());
                  Type listType = new TypeToken<List<Auction>>() {}.getType();
                  List<Auction> fetched = gson.fromJson(jsonData, listType);

                  Platform.runLater(
                      () -> {
                        pendingAuctionList.setAll(fetched);
                        itemTable.setItems(pendingAuctionList);
                        clearPreview();
                      });
                }
              } catch (Exception e) {
                Platform.runLater(
                    () -> showAlert("Lỗi", "Không thể tải dữ liệu: " + e.getMessage()));
              }
            })
        .start();
  }

  // =======================================================
  // 3. XỬ LÝ DUYỆT / TỪ CHỐI (KÍCH HOẠT SCHEDULER)
  // =======================================================
  @FXML
  private void handleApprove(ActionEvent event) {
    Auction selected = itemTable.getSelectionModel().getSelectedItem();
    if (selected == null) return;

    JsonObject data = new JsonObject();
    data.addProperty("auctionId", selected.getAuctionId());
    Request request = new Request("APPROVE_AUCTION", data);

    new Thread(
            () -> {
              try {
                String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
                Response res = gson.fromJson(jsonResponse, Response.class);
                Platform.runLater(
                    () -> {
                      if ("SUCCESS".equals(res.getStatus())) {
                        showAlert(
                            "Thành công",
                            "Đã duyệt! Phòng sẽ tự mở vào lúc: " + selected.getStartTime());
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
