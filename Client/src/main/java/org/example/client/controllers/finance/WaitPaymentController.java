package org.example.client.controllers.finance;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import org.example.client.controllers.BaseController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.itemsDTO.PendingItemsDTO;
import org.example.core.dto.paymentDTO.PendingPaymentsDTO;
import org.example.core.models.users.User;
import org.example.core.shared.enums.ActionType;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller quản lý màn hình giỏ hàng và danh sách các sản phẩm đang chờ thanh toán (Wait
 * Payment). Hỗ trợ các chức năng xử lý thanh toán đơn lẻ từng món đồ hoặc quyết toán hàng loạt (Pay
 * All).
 */
public class WaitPaymentController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(WaitPaymentController.class.getName());

  @FXML private TableView<PendingPaymentsDTO> tvPendingItems;
  @FXML private TableColumn<PendingPaymentsDTO, String> colName;
  @FXML private TableColumn<PendingPaymentsDTO, BigDecimal> colPrice;
  @FXML private TableColumn<PendingPaymentsDTO, String> colDate;
  @FXML private TableColumn<PendingPaymentsDTO, Void> colAction;
  @FXML private Button btnPayAll;

  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();
  private final ObservableList<PendingPaymentsDTO> observableList =
      FXCollections.observableArrayList();
  private final User currentUser = UserSession.getInstance().getCurrentUser();

  /**
   * Phương thức khởi chạy của JavaFX, liên kết nhà máy cellFactory cho cột dữ liệu và cột thao tác
   * nút bấm.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    colName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
    colPrice.setCellValueFactory(new PropertyValueFactory<>("winPrice"));
    colDate.setCellValueFactory(new PropertyValueFactory<>("endDate"));

    colPrice.setCellFactory(
        column ->
            new TableCell<>() {
              @Override
              protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                  setText(null);
                } else {
                  setText(String.format("%,d VNĐ", price.longValue()));
                }
              }
            });

    setupActionColumn();
    loadPendingItems();
  }

  /** Sự kiện nút bấm kích hoạt quyết toán toàn bộ các sản phẩm đã đấu giá thắng trong danh sách. */
  @FXML
  public void handlePayAll(ActionEvent event) {
    if (observableList.isEmpty()) {
      showAlert("Thông báo", "Giỏ hàng trống, không có gì để thanh toán!");
      return;
    }

    BigDecimal totalAmount =
        observableList.stream()
            .map(PendingPaymentsDTO::getWinPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Xác nhận thanh toán tất cả");
    confirmAlert.setHeaderText("Thanh toán toàn bộ danh sách chờ?");
    confirmAlert.setContentText("Tổng số tiền: " + String.format("%,.0f VNĐ", totalAmount));

    confirmAlert
        .showAndWait()
        .ifPresent(
            response -> {
              if (response == ButtonType.OK) {
                sendPayAllRequest();
              }
            });
  }

  /**
   * Khởi tạo giao diện đồ họa cho cột Thao tác, tự động bơm nút bấm "Thanh toán" độc lập vào từng
   * hàng.
   */
  private void setupActionColumn() {
    Callback<TableColumn<PendingPaymentsDTO, Void>, TableCell<PendingPaymentsDTO, Void>>
        cellFactory =
            new Callback<>() {
              @Override
              public TableCell<PendingPaymentsDTO, Void> call(
                  final TableColumn<PendingPaymentsDTO, Void> param) {
                return new TableCell<>() {
                  private final Button btnPay = new Button("Thanh toán");

                  {
                    btnPay.setStyle(
                        "-fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5; -fx-font-weight: bold;");
                    btnPay.setOnAction(
                        (ActionEvent event) -> {
                          PendingPaymentsDTO data = getTableView().getItems().get(getIndex());
                          if (currentUser != null) {
                            data.setUserId(currentUser.getUserId());
                          }
                          handleSinglePayment(data);
                        });
                  }

                  @Override
                  protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                      setGraphic(null);
                    } else {
                      setGraphic(btnPay);
                      setStyle("-fx-alignment: CENTER;");
                    }
                  }
                };
              }
            };
    colAction.setCellFactory(cellFactory);
  }

  /**
   * Luồng chạy ngầm gửi tín hiệu lên máy chủ yêu cầu lấy danh sách các phiên thắng cuộc đang chờ
   * thanh toán.
   */
  private void loadPendingItems() {
    if (currentUser == null) {
      showAlert("Lỗi", "Vui lòng đăng nhập lại!");
      return;
    }

    int userId = currentUser.getUserId();
    PendingItemsDTO payload = new PendingItemsDTO(userId);
    payload.setSellerId(userId);

    Request request = new Request(ActionType.GET_PENDING_PAYMENTS, payload.getSellerId());
    String jsonRequest = gson.toJson(request);

    new Thread(
            () -> {
              try {
                logger.info("Đang truy vấn danh sách hóa đơn chờ thanh toán từ Server...");
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);

                Platform.runLater(
                    () -> {
                      if ("SUCCESS".equals(response.getStatus())) {
                        String jsonData = gson.toJson(response.getData());
                        java.lang.reflect.Type listType =
                            new com.google.gson.reflect.TypeToken<
                                List<PendingPaymentsDTO>>() {}.getType();
                        List<PendingPaymentsDTO> fetchedItems = gson.fromJson(jsonData, listType);

                        observableList.setAll(fetchedItems);
                        tvPendingItems.setItems(observableList);
                        logger.log(
                            Level.INFO,
                            "Đã cập nhật thành công {0} hóa đơn vào TableView.",
                            fetchedItems.size());
                      } else {
                        showAlert("Lỗi", response.getMessage());
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(
                    () ->
                        showAlert(
                            "Lỗi kết nối", "Không thể lấy dữ liệu thanh toán: " + e.getMessage()));
                logger.log(
                    Level.SEVERE, "Lỗi xảy ra khi nạp dữ liệu thanh toán từ máy chủ qua Socket", e);
              }
            })
        .start();
  }

  /** Hiển thị bảng xác nhận Dialog trước khi thực hiện thanh toán cho một mặt hàng đơn lẻ. */
  private void handleSinglePayment(PendingPaymentsDTO itemToPay) {
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Xác nhận thanh toán");
    confirmAlert.setHeaderText(
        "Bạn có chắc chắn muốn thanh toán cho: " + itemToPay.getItemName() + "?");
    confirmAlert.setContentText(
        "Số tiền sẽ trừ vào tài khoản: " + String.format("%,.0f VNĐ", itemToPay.getWinPrice()));

    confirmAlert
        .showAndWait()
        .ifPresent(
            response -> {
              if (response == ButtonType.OK) {
                sendPaymentRequest(itemToPay);
              }
            });
  }

  /** Bắn gói tin yêu cầu trừ tiền tài khoản cho một vật phẩm đấu giá lên máy chủ. */
  private void sendPaymentRequest(PendingPaymentsDTO itemToPay) {
      Request request = new Request(ActionType.PAY_ITEM, itemToPay);

      new Thread(() -> {
          try {
              String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
              Response serverResponse = gson.fromJson(jsonResponse, Response.class);

              Platform.runLater(() -> {
                  if ("SUCCESS".equals(serverResponse.getStatus())) {
                      showAlert("Thành công", "Thanh toán thành công! Số dư đã được cập nhật.");
                      observableList.remove(itemToPay);
                  } else {
                      int code = serverResponse.getData() instanceof Number ? ((Number) serverResponse.getData()).intValue() : -1;
                      String title = switch (code) {
                          case 4001 -> "Số dư ví không đủ (4001)";
                          case 4090 -> "Trạng thái không hợp lệ (409)";
                          case 5000 -> "Lỗi cơ sở dữ liệu (500)";
                          default -> "Thanh toán thất bại (" + code + ")";
                      };
                      showAlert(title, serverResponse.getMessage());
                  }
              });
          } catch (Exception e) {
              Platform.runLater(() -> showAlert("Lỗi kết nối", "Không thể gửi yêu cầu thanh toán."));
          }
      }).start();
  }

  /** Gửi yêu cầu xóa nợ và thanh toán đồng loạt cho toàn bộ các vật phẩm đã thắng. */
  private void sendPayAllRequest() {
      if (currentUser == null) return;
      Request request = new Request(ActionType.PAY_ALL, currentUser.getUserId());

      new Thread(() -> {
          try {
              String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
              Response serverResponse = gson.fromJson(jsonResponse, Response.class);

              Platform.runLater(() -> {
                  if ("SUCCESS".equals(serverResponse.getStatus())) {
                      showAlert("Thành công", "Đã thanh toán tất cả sản phẩm!");
                      observableList.clear();
                  } else {
                      int code = serverResponse.getData() instanceof Number ? ((Number) serverResponse.getData()).intValue() : -1;
                      String title = (code == 4001) ? "Số dư ví không đủ (4001)" : "Thanh toán thất bại (" + code + ")";
                      showAlert(title, serverResponse.getMessage());
                  }
              });
          } catch (Exception e) {
              Platform.runLater(() -> showAlert("Lỗi", "Kết nối Server thất bại"));
          }
      }).start();
  }
}
