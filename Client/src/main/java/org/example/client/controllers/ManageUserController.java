package org.example.client.controllers;

import com.google.gson.Gson;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.core.models.users.User;
import org.example.core.shared.enums.RoleType;
import org.example.core.shared.enums.UserStatus;

import java.math.BigDecimal;
import java.net.URL;
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
        // TODO: Gửi request lên Server lấy danh sách User

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
        // TODO: Gửi socket request lên server
    }

    @FXML
    public void handleUnbanUser(ActionEvent event) {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert( "Cảnh báo", "Vui lòng chọn một người dùng trong bảng để mở khóa!");
            return;
        }

        System.out.println("Gửi lệnh UNBAN user ID: " + selectedUser.getUserId() + " lên server...");
        // TODO: Gửi socket request lên server
    }
}