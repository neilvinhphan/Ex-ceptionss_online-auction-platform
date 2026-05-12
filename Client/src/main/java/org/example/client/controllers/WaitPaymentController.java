package org.example.client.controllers;

import com.google.gson.Gson;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.PendingPaymentsDTO;
import org.example.core.dto.PendingItemsDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.users.User;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

public class WaitPaymentController extends BaseController implements Initializable {
    // --- CÁC THÀNH PHẦN BẢNG ---
    @FXML
    private TableView<PendingPaymentsDTO> tvPendingItems;
    @FXML
    private TableColumn<PendingPaymentsDTO, String> colName;
    @FXML
    private TableColumn<PendingPaymentsDTO, BigDecimal> colPrice;
    @FXML
    private TableColumn<PendingPaymentsDTO, String> colDate;
    @FXML
    private TableColumn<PendingPaymentsDTO, Void> colAction; // Cột chứa nút bấm dùng kiểu Void

    @FXML
    private MenuButton menuUser;
    @FXML
    private MenuButton menuDanhMuc;
    @FXML
    private Button btnPayAll;
    private Gson gson = ClientManager.getInstance().getGson();
    private final AuctionClient clientSocket = ClientManager.getInstance().getClient();
    private ObservableList<PendingPaymentsDTO> observableList = FXCollections.observableArrayList();
    User currentUser = UserSession.getInstance().getCurrentUser();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (currentUser != null) {
            menuUser.setText(currentUser.getUserName());
        }
        // 1. Cấu hình các cột hiển thị dữ liệu bình thường
        colName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("winPrice"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        // Format cột giá tiền cho đẹp (vd: 5,000,000)
        colPrice.setCellFactory(column -> new TableCell<>() {
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
        // 2. Cấu hình CỘT THAO TÁC (Chứa nút Thanh toán)
        setupActionColumn();
        // 3. Tải dữ liệu vào bảng
        loadPendingItems();
    }

    /**
     * Hàm này tạo nút "Thanh toán" cho từng dòng trong bảng
     */
    private void setupActionColumn() {
        Callback<TableColumn<PendingPaymentsDTO, Void>, TableCell<PendingPaymentsDTO, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<PendingPaymentsDTO, Void> call(final TableColumn<PendingPaymentsDTO, Void> param) {
                return new TableCell<>() {
                    private final Button btnPay = new Button("Thanh toán");

                    {
                        btnPay.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5; -fx-font-weight: bold;");
                        btnPay.setOnAction((ActionEvent event) -> {
                            // Lấy dữ liệu của dòng hiện tại đang được click
                            PendingPaymentsDTO data = getTableView().getItems().get(getIndex());
                            data.setUserId(currentUser.getUserId());
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

    // --- CÁC HÀM XỬ LÝ LOGIC ---
    private void loadPendingItems() {
            User currentUser = UserSession.getInstance().getCurrentUser();
            if (currentUser == null) {
                showAlert("Lỗi", "Vui lòng đăng nhập lại!");
                return;
            }
            int userId = currentUser.getUserId();
            PendingItemsDTO payload = new PendingItemsDTO(userId);
            System.out.println("Id nguoi dung la " + payload.getSellerId());
            Request request = new Request("GET_PENDING_PAYMENTS", payload.getSellerId());
            String jsonRequest = gson.toJson(request);

            new Thread(() -> {
                try {
                    System.out.println("Tao request PendingItems WaitPayment");
                    String jsonResponse = clientSocket.sendRequest(jsonRequest);
                    Response response = gson.fromJson(jsonResponse, Response.class);
                    System.out.println("Nhan response PendingItems WaitPayment");

                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                            String jsonData = gson.toJson(response.getData());
                            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<PendingPaymentsDTO>>(){}.getType();
                            List<PendingPaymentsDTO> fetchedItems = gson.fromJson(jsonData, listType);
                            observableList.setAll(fetchedItems);
                            tvPendingItems.setItems(observableList);
                            System.out.println("Đã tải: " + fetchedItems.size() + " mục chờ thanh toán.");
                        } else {
                            showAlert("Lỗi", response.getMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Lỗi kết nối", "Không thể lấy dữ liệu thanh toán: " + e.getMessage()));
                    e.printStackTrace();
                }
            }).start();
        }

    private void handleSinglePayment(PendingPaymentsDTO itemToPay) {
        // 1. Hiển thị bảng thông báo xác nhận
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận thanh toán");
        confirmAlert.setHeaderText("Bạn có chắc chắn muốn thanh toán cho: " + itemToPay.getItemName() + "?");
    System.out.println("Thong tin vat pham " + itemToPay.getAuctionId() + " " + itemToPay.getUserId());
        confirmAlert.setContentText("Số tiền sẽ trừ vào tài khoản: " + String.format("%,.0f VNĐ", itemToPay.getWinPrice()));
        // 2. Chờ người dùng phản hồi
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // 3. Nếu bấm OK -> Gửi lệnh thanh toán lên Server
                sendPaymentRequest(itemToPay);
            }
        });
    }

    private void sendPaymentRequest(PendingPaymentsDTO itemToPay) {
        // Tạo Request với Action "PAY_ITEM"
        Request request = new Request("PAY_ITEM", itemToPay);
        String jsonRequest = gson.toJson(request);

        new Thread(() -> {
            try {
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response serverResponse = gson.fromJson(jsonResponse, Response.class);

                Platform.runLater(() -> {
                    if ("SUCCESS".equals(serverResponse.getStatus())) {
                        showAlert("Thành công", "Thanh toán thành công! Số dư đã được cập nhật.");
                        // Xóa món đồ ra khỏi bảng vì đã thanh toán xong
                        observableList.remove(itemToPay);
                    } else {
                        showAlert("Lỗi thanh toán", serverResponse.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối", "Không thể gửi yêu cầu thanh toán."));
            }
        }).start();
    }

    @FXML
    public void handlePayAll(ActionEvent event) {
        if (observableList.isEmpty()) {
            showAlert("Thông báo", "Giỏ hàng trống, không có gì để thanh toán!");
            return;
        }
        // 1. Tính tổng tiền để để thông báo cho người dùng
        BigDecimal totalAmount = observableList.stream()
                .map(PendingPaymentsDTO::getWinPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 2. Hiện Alert xác nhận tổng quát
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận thanh toán tất cả");
        confirmAlert.setHeaderText("Thanh toán toàn bộ danh sách chờ?");
        confirmAlert.setContentText("Tổng số tiền: " + String.format("%,.0f VNĐ", totalAmount));
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                sendPayAllRequest();
            }
        });
    }

    private void sendPayAllRequest() {
        // Gửi ID người dùng để Server tự xử lý thanh toán mọi món đồ đã thắng của người này
        int userId = UserSession.getInstance().getCurrentUser().getUserId();
        Request request = new Request("PAY_ALL", userId);

        new Thread(() -> {
            try {
                String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
                Response serverResponse = gson.fromJson(jsonResponse, Response.class);

                Platform.runLater(() -> {
                    if ("SUCCESS".equals(serverResponse.getStatus())) {
                        showAlert("Thành công", "Đã thanh toán tất cả sản phẩm!");
                        observableList.clear(); // Xóa sạch bảng
                    } else {
                        showAlert("Lỗi", serverResponse.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi", "Kết nối Server thất bại"));
            }
        }).start();
    }

    @FXML
    public void handleMain(ActionEvent event) {
        switchScene(event, "/views/MainView.fxml", "Trang chu");
    }

    @FXML
    public void handleMenuItem(ActionEvent event) {
        switchScene(event, "/views/AuctionCatalogView.fxml", "Danh sach cuoc dau gia");
    }

    @FXML
    public void handleHistoryAuction(ActionEvent event) {
        switchScene(event, "/views/AuctionHistoryView.fxml", "Lich su dau gia");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        switchScene(event, "/views/LoginView.fxml", "Dang nhap he thong");
    }

    @FXML
    public void handleUserUi(ActionEvent event) {
        switchScene(event, "/views/PersonalView.fxml", "Hồ sơ cá nhân");
    }

    @FXML
    public void handleWareHouse(ActionEvent event) {
        switchScene(event, "/views/WareHouseView.fxml", "Kho hàng");
    }

    @FXML
    public void handleCreateItem(ActionEvent event) {
        switchScene(event, "/views/CreateItemView.fxml", "Tao san pham dau gia");
    }

    @FXML
    public void handleCreateAuction(ActionEvent event) {
        switchScene(event, "/views/CreateAuctionView.fxml", "Tao cuoc dau gia moi");
    }
}
