package org.example.client.controllers.admin;

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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.lang.reflect.Type;
import java.net.URL;
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
import org.example.core.dto.admin.AdminCancelAuctionDTO;
import org.example.core.models.entities.Auction;
import org.example.core.models.items.Item;
import org.example.core.shared.enums.ActionType;

/**
 * Controller xử lý quy trình phê duyệt hoặc từ chối các phiên đấu giá đang chờ duyệt từ phía Admin.
 */
public class AuctionApprovalController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(AuctionApprovalController.class.getName());

  @FXML private TableView<Auction> itemTable;
  @FXML private TableColumn<Auction, Integer> colId;
  @FXML private TableColumn<Auction, String> colItemName;
  @FXML private TableColumn<Auction, String> colType;
  @FXML private TableColumn<Auction, String> colPrice;
  @FXML private ImageView itemImageView;
  @FXML private Label lblNoImage;
  @FXML private Label lblName;
  @FXML private Label lblType;
  @FXML private Label lblPrice;
  @FXML private TextArea txtDescription;
  @FXML private Button btnApprove;
  @FXML private Button btnReject;

  private final ObservableList<Auction> pendingAuctionList = FXCollections.observableArrayList();
  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  /**
   * Khởi tạo cấu trúc bảng dữ liệu, tải danh sách chờ duyệt và đăng ký lắng nghe sự kiện chọn dòng.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    setupTableColumns();
//    loadPendingAuctions();

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

  /** Định hình cấu trúc ánh xạ dữ liệu của thực thể Auction và Item lên các cột của TableView. */
  private void setupTableColumns() {
    colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));

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

  /**
   * Trích xuất thông tin chi tiết của phiên đấu giá được chọn và giải mã chuỗi Base64 để hiển thị
   * ảnh.
   *
   * @param auction Đối tượng phiên đấu giá được chọn từ bảng.
   */
  private void showAuctionPreview(Auction auction) {
    Item item = auction.getItem();
    if (item == null) return;

    lblName.setText(item.getItemName());
    lblType.setText(item.getType());
    lblPrice.setText(String.format("%,d VND", item.getStartingPrice().longValue()));
    txtDescription.setText(item.getDescription());

    if (item.getImage() != null && !item.getImage().isEmpty()) {
      lblNoImage.setVisible(false);
      new Thread(
              () -> {
                try {
                  Image img = ImageUtils.decodeBase64ToImage(item.getImage());
                  Platform.runLater(() -> itemImageView.setImage(img));
                } catch (Exception e) {
                  logger.log(Level.WARNING, "Không thể giải mã hình ảnh Base64 của sản phẩm", e);
                  Platform.runLater(
                      () -> {
                        itemImageView.setImage(null);
                        lblNoImage.setVisible(true);
                        lblNoImage.setText("Lỗi tải ảnh");
                      });
                }
              })
          .start();
    } else {
      itemImageView.setImage(null);
      lblNoImage.setVisible(true);
      lblNoImage.setText("Không có ảnh");
    }
  }

  /** Khôi phục trạng thái trống cho vùng hiển thị thông tin xem trước (Preview) sản phẩm. */
  private void clearPreview() {
    lblName.setText("...");
    lblType.setText("...");
    lblPrice.setText("...");
    txtDescription.setText("");
    itemImageView.setImage(null);
    lblNoImage.setVisible(true);
    lblNoImage.setText("Không có ảnh");
  }

//  /**
//   * Làm mới lại danh sách các phiên đấu giá đang chờ duyệt.
//   *
//   * @param event Sự kiện kích hoạt từ UI.
//   */
//  @FXML
//  private void handleRefresh(ActionEvent event) {
//    loadPendingAuctions();
//  }

//  /**
//   * Tạo tiến trình bất đồng bộ gửi yêu cầu lên Server để đồng bộ danh sách các phiên đấu giá chờ
//   * duyệt.
//   */
//  private void loadPendingAuctions() {
//    Request request = new Request(ActionType.GET_PENDING_AUCTIONS, null);
//
//    new Thread(
//            () -> {
//              try {
//                logger.info("Đang gửi yêu cầu lấy danh sách phiên đấu giá chờ duyệt lên Server.");
//                String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
//                logger.fine("Dữ liệu phản hồi từ Server: " + jsonResponse);
//
//                Response response = gson.fromJson(jsonResponse, Response.class);
//
//                if ("SUCCESS".equals(response.getStatus())) {
//                  String jsonData = gson.toJson(response.getData());
//                  Type listType = new TypeToken<List<Auction>>() {}.getType();
//                  List<Auction> fetched = gson.fromJson(jsonData, listType);
//
//                  Platform.runLater(
//                      () -> {
//                        pendingAuctionList.setAll(fetched);
//                        itemTable.setItems(pendingAuctionList);
//                        clearPreview();
//                      });
//                } else {
//                  logger.warning(
//                      "Server từ chối cung cấp danh sách chờ duyệt: " + response.getMessage());
//                  Platform.runLater(() -> showAlert("Lỗi phản hồi", response.getMessage()));
//                }
//              } catch (Exception e) {
//                logger.log(
//                    Level.SEVERE, "Lỗi nghiêm trọng khi tải danh sách phiên đấu giá chờ duyệt", e);
//                Platform.runLater(
//                    () -> showAlert("Lỗi kết nối", "Chi tiết lỗi: " + e.getMessage()));
//              }
//            })
//        .start();
//  }

//  /**
//   * Xử lý phê duyệt phiên đấu giá đã chọn, lên lịch mở phòng đấu giá tự động theo thời gian cấu
//   * hình.
//   *
//   * @param event Sự kiện kích hoạt từ UI.
//   */
//  @FXML
//  private void handleApprove(ActionEvent event) {
//    Auction selected = itemTable.getSelectionModel().getSelectedItem();
//    if (selected == null) return;
//
//    JsonObject data = new JsonObject();
//    data.addProperty("auctionId", selected.getAuctionId());
//    Request request = new Request(ActionType.APPROVE_AUCTION, data);
//
//    new Thread(
//            () -> {
//              try {
//                logger.info("Gửi yêu cầu phê duyệt phiên đấu giá ID: " + selected.getAuctionId());
//                String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
//                Response res = gson.fromJson(jsonResponse, Response.class);
//
//                Platform.runLater(
//                    () -> {
//                      if ("SUCCESS".equals(res.getStatus())) {
//                        showAlert(
//                            "Thành công",
//                            "Đã duyệt! Phòng sẽ tự mở vào lúc: " + selected.getStartTime());
//                        loadPendingAuctions();
//                      } else {
//                        logger.warning("Phê duyệt thất bại: " + res.getMessage());
//                        showAlert("Lỗi", res.getMessage());
//                      }
//                    });
//              } catch (Exception e) {
//                logger.log(
//                    Level.SEVERE,
//                    "Lỗi mạng khi phê duyệt phiên đấu giá ID: " + selected.getAuctionId(),
//                    e);
//                Platform.runLater(
//                    () -> showAlert("Lỗi kết nối", "Chi tiết lỗi: " + e.getMessage()));
//              }
//            })
//        .start();
//  }

  /**
   * Xử lý từ chối và hủy bỏ phiên đấu giá đang được lựa chọn từ quản trị viên.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  private void handleReject(ActionEvent event) {
      Auction selected = itemTable.getSelectionModel().getSelectedItem();
      if (selected == null) return;

      int adminId = UserSession.getInstance().getCurrentUser().getUserId();
      AdminCancelAuctionDTO cancelReq = new AdminCancelAuctionDTO(selected.getAuctionId(), adminId);
      Request request = new Request(ActionType.ADMIN_CANCEL_AUCTION, cancelReq);

      new Thread(
              () -> {
                  try {
                      logger.info("Gửi yêu cầu hủy phiên đấu giá khẩn cấp từ Admin ID " + adminId);
                      String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
                      Response res = gson.fromJson(jsonResponse, Response.class);

                      Platform.runLater(() -> {
                          if ("SUCCESS".equals(res.getStatus())) {
                              showAlert("Thành công", "Đã cưỡng chế hủy / từ chối phiên đấu giá.");
                          } else {
                              int code = res.getData() instanceof Number ? ((Number) res.getData()).intValue() : -1;
                              String errorTitle = switch (code) {
                                  case 4040 -> "Không tìm thấy phiên (404)";
                                  case 4030 -> "Từ chối quyền hạn Admin (403)";
                                  case 5000 -> "Lỗi hạ tầng cơ sở dữ liệu (500)";
                                  default -> "Hủy phiên thất bại (" + code + ")";
                              };
                              logger.warning("Admin từ chối phiên thất bại [" + code + "]: " + res.getMessage());
                              showAlert(errorTitle, res.getMessage());
                          }
                      });
                  } catch (Exception e) {
                      logger.log(Level.SEVERE, "Lỗi hệ thống khi gửi yêu cầu từ chối phiên đấu giá", e);
                      Platform.runLater(() -> showAlert("Lỗi mạng", "Chi tiết lỗi: " + e.getMessage()));
                  }
              })
              .start();
  }

  /**
   * Quay lại màn hình giao diện quản lý phiên đấu giá tổng quan.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  private void handleBack(ActionEvent event) {
    switchScene(event, "/views/ManageAuctionView.fxml", "Quản trị hệ thống");
  }
}
