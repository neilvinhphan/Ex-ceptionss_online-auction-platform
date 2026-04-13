package org.example.server.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

public class MainController extends BaseController {
    @FXML
    private MenuButton menuDanhMuc;
    @FXML
    private MenuButton menuPhongDauGia;
    @FXML
    private MenuButton menuSearch;

    @FXML
    private void handleMenuItem(ActionEvent event) {
        MenuItem item = (MenuItem) event.getSource();

        // Lấy ra MenuButton cha của MenuItem đó
        // Lưu ý: Phải ép kiểu về MenuButton để set lại Text
        MenuButton parentMenu = (MenuButton) item.getParentPopup().getOwnerNode();

        // Cập nhật Text của nút cha bằng Text của item đã chọn
        parentMenu.setText(item.getText());
    }

    @FXML
    void handleMain(ActionEvent event) {

        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    @FXML
    void handleLogin(ActionEvent event) {
        switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
    }

    @FXML
    void handleRegister(ActionEvent event) {
        switchScene(event, "/views/RegisterView.fxml", "Đăng ký tài khoản");
    }

    @FXML
    void handleUser(ActionEvent event) {
        System.out.println("Về trang thông tin cá nhân");
    }
}
