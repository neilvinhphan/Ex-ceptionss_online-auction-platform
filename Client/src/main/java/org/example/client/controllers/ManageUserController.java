package org.example.client.controllers;

import com.google.gson.Gson;

import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.admin.AdminBanUserDTO;
import org.example.core.models.users.User;
import org.example.core.shared.enums.RoleType;
import org.example.core.shared.enums.UserStatus;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ManageUserController extends BaseController implements Initializable {
    @FXML private TextField searchField;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colPhone;
    @FXML private TableColumn<User, RoleType> colRole;
    @FXML private TableColumn<User, UserStatus> colStatus;
    @FXML private TableColumn<User, BigDecimal> colBalance;
    // Công cụ gọi Server
    private Gson gson = ClientManager.getInstance().getGson();
    private final AuctionClient clientSocket = ClientManager.getInstance().getClient();
    // Danh sách chứa dữ liệu hiển thị lên bảng
    private ObservableList<User> userList = FXCollections.observableArrayList();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Ánh xạ các cột trong bảng với các thuộc tính của class User
        colId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        // Load dữ liệu lần đầu khi mở trang
        loadUsersFromServer();
    }
    private void loadUsersFromServer() {
        new Thread(() -> {
            try {
                // 1. Lấy ID của Admin đang đăng nhập
                int adminId = UserSession.getInstance().getCurrentUser().getUserId();

                // 2. Đóng gói Request gửi lên Server
                Request request = new Request("ADMIN_GET_ALL_USERS", adminId);
                clientSocket.getOut().println(gson.toJson(request));

                // 3. Đọc dữ liệu Server trả về
                String responseStr = clientSocket.getIn().readLine();
                if (responseStr != null) {
                    Response response = gson.fromJson(responseStr, Response.class);

                    if ("SUCCESS".equals(response.getStatus())) {
                        // 4. Bóc tách mảng (List) User từ data của Response
                        String dataJson = gson.toJson(response.getData());
                        Type listType = new TypeToken<List<User>>(){}.getType();
                        List<User> users = gson.fromJson(dataJson, listType);

                        // 5. Cập nhật giao diện (Phải dùng Platform.runLater vì đang ở luồng ngầm)
                        Platform.runLater(() -> {
                            userList.setAll(users);
                            userTable.setItems(userList);
                            System.out.println("Đã tải xong danh sách người dùng!");
                        });
                    } else {
                        Platform.runLater(() -> showAlert("Lỗi", response.getMessage()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Lỗi mạng", "Không thể kết nối đến Server!"));
            }
        }).start();

    }

    @FXML
    public void handleBack(ActionEvent event) {
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    @FXML
    public void handleSearch(ActionEvent event) {
            String keyword = searchField.getText().trim().toLowerCase();
            System.out.println("Đang tìm kiếm từ khóa: " + keyword);

            // 1. Nếu người dùng xóa trắng ô tìm kiếm và bấm nút, trả lại danh sách gốc
            if (keyword.isEmpty()) {
                userTable.setItems(userList);
                return;
            }
            // 2. Tạo một giỏ hàng tạm để chứa những người dùng khớp với từ khóa
            ObservableList<User> filteredList = FXCollections.observableArrayList();

            // 3. Duyệt qua từng người trong danh sách gốc
            for (User user : userList) {
                boolean matchUsername = user.getUserName() != null && user.getUserName().toLowerCase().contains(keyword);
                boolean matchEmail = user.getEmail() != null && user.getEmail().toLowerCase().contains(keyword);
                boolean matchPhone = user.getPhone() != null && user.getPhone().toLowerCase().contains(keyword);

                // Nếu khớp 1 trong 3 điều kiện trên thì nhét vào giỏ hàng tạm
                if (matchUsername || matchEmail || matchPhone) {
                    filteredList.add(user);
                }
            }

            // 4. Cập nhật bảng để hiển thị giỏ hàng tạm
            userTable.setItems(filteredList);

            // (Tùy chọn) Hiển thị thông báo nếu không tìm thấy ai
            if (filteredList.isEmpty()) {
                showAlert("Thong bao!", "Khong tim thay nguoi dung phu hop");
            }
        }

    @FXML
    public void handleRefresh(ActionEvent event) {
        searchField.clear();
        loadUsersFromServer();
        System.out.println("Đã làm mới danh sách!");
    }

    @FXML
    public void handleBanUser(ActionEvent event) {
        // Lấy user đang được click chọn trong bảng
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("Cảnh báo", "Vui lòng chọn một người dùng trong bảng để khóa!");
            return;
        }

        System.out.println("Gửi lệnh BAN user ID: " + selectedUser.getUserId() + " lên server...");

        new Thread(() -> {
            try {
                int adminId = UserSession.getInstance().getCurrentUser().getUserId();

                // true = Khóa tài khoản
                AdminBanUserDTO banDto = new AdminBanUserDTO(adminId, selectedUser.getUserId(), true);
                Request request = new Request("ADMIN_BAN_USER", banDto);

                clientSocket.getOut().println(gson.toJson(request));

                String responseStr = clientSocket.getIn().readLine();
                if (responseStr != null) {
                    Response response = gson.fromJson(responseStr, Response.class);

                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                            showAlert("Thành công", response.getMessage());
                            loadUsersFromServer(); // Tải lại bảng để cập nhật trạng thái mới
                        } else {
                            showAlert("Lỗi", response.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleUnbanUser(ActionEvent event) {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert( "Cảnh báo", "Vui lòng chọn một người dùng trong bảng để mở khóa!");
            return;
        }

        System.out.println("Gửi lệnh UNBAN user ID: " + selectedUser.getUserId() + " lên server...");

        new Thread(() -> {
            try {
                int adminId = UserSession.getInstance().getCurrentUser().getUserId();

                // false = Mở khóa tài khoản
                AdminBanUserDTO unbanDto = new AdminBanUserDTO(adminId, selectedUser.getUserId(), false);
                Request request = new Request("ADMIN_BAN_USER", unbanDto);

                clientSocket.getOut().println(gson.toJson(request));

                String responseStr = clientSocket.getIn().readLine();
                if (responseStr != null) {
                    Response response = gson.fromJson(responseStr, Response.class);

                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                            showAlert("Thành công", response.getMessage());
                            loadUsersFromServer(); // Tải lại bảng để cập nhật trạng thái mới
                        } else {
                            showAlert("Lỗi", response.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}