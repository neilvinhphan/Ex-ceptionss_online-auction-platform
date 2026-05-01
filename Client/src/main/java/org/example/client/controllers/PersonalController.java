package org.example.client.controllers;

import org.example.client.utils.UserSession;
import org.example.core.models.users.User;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;

public class PersonalController extends BaseController implements Initializable {
    @FXML
    private MenuButton menuDanhMuc;
    @FXML
    private MenuButton menuPhongDauGia;
    @FXML
    private MenuButton menuUser;
    @FXML
    private Label lbUserName;
    @FXML
    private Label lbPhoneNum;
    @FXML
    private Label lbEmail;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            menuUser.setText(currentUser.getUserName());
            lbUserName.setText(currentUser.getUserName());
            lbPhoneNum.setText(currentUser.getPhone());
            lbEmail.setText(currentUser.getEmail());
        }
    }

    @FXML
    private Button createAuction;

    @FXML
    private void handleMenuItem(ActionEvent event) {
        MenuItem item = (MenuItem) event.getSource();
        MenuButton parentMenu = (MenuButton) item.getParentPopup().getOwnerNode();
        parentMenu.setText(item.getText());
        switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục sản phẩm đấu giá");
    }

    @FXML
    private void handleRoomAuction(ActionEvent event) {
        MenuItem item = (MenuItem) event.getSource();
        MenuButton parentMenu = (MenuButton) item.getParentPopup().getOwnerNode();
        parentMenu.setText(item.getText());
    }

    @FXML
    void handleCreateAuction(ActionEvent event) {
        switchScene(event, "/views/CreateAuctionView.fxml", "Tạo cuộc đấu giá");
    }

    @FXML
    void handleEditPhone(ActionEvent event) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        TextInputDialog dialog = new TextInputDialog(currentUser.getPhone());
        dialog.setTitle("Cập nhật thông tin");
        dialog.setHeaderText("Chỉnh sửa số điện thoại");
        dialog.setContentText("Nhập số điện thoại mới của bạn:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String newPhone = result.get().trim();
            int currentUserId = currentUser.getUserId();
            if (true) {
                currentUser.setPhone(newPhone);
                lbPhoneNum.setText(newPhone);
                showAlert("Thành công", "Đã cập nhật số điện thoại mới vào Database!");
            } else {
                showAlert("Lỗi", "Không thể cập nhật số điện thoại vào Database!");
            }
        }
    }

    @FXML
    void handleEditEmail(ActionEvent event) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        TextInputDialog dialog = new TextInputDialog(currentUser.getEmail());
        dialog.setTitle("Cập nhật thông tin");
        dialog.setHeaderText("Chỉnh sửa Email");
        dialog.setContentText("Nhập Email mới của bạn:");
        // Lấy kết quả người dùng nhập
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String newEmail = result.get().trim();
            int currentUserId = currentUser.getUserId();
            if (true) {
                currentUser.setEmail(newEmail);
                lbEmail.setText(newEmail);
                showAlert("Thành công", "Đã cập nhật số điện thoại mới vào Database!");
            } else {
                showAlert("Lỗi", "Không thể cập nhật số điện thoại vào Database!");
            }
        }
    }

    @FXML
    void handleMain(ActionEvent event) {
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        UserSession.getInstance().cleanUserSession();
        switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
    }

    @FXML
    void handleUserUi(ActionEvent event) {
        switchScene(event, "/views/PersonalView.fxml", "Hồ sơ cá nhân");
    }

}
