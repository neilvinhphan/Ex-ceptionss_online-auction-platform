package org.example.server.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController extends BaseController {

    @FXML
    private TextField tfuserName;
    @FXML
    private PasswordField pass_an;
    @FXML
    private TextField pass_hien;

    @FXML
    void handleLogin(ActionEvent event) {
        String userName = tfuserName.getText();
        String password = pass_an.getText();
        String passwordhidden = pass_hien.getText();

        if (userName.isEmpty() || password.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");

        }

        /* Giả lập kiểm tra tài khoản (Sau này bạn sẽ gọi vào Database ở đây)
        if (userName.equals("admin") && password.equals("123")) {
            System.out.println("Đăng nhập thành công!");
        } else {
            showAlert("Thất bại", "Tài khoản hoặc mật khẩu không chính xác!");
        }
         */
        else {
            System.out.println("Chuyển sang trang chủ");
            switchScene(event, "/views/MainView.fxml", "Trang chủ");
        }
    }

    @FXML
    void hienthi_pass(ActionEvent event) {
        logichienthi_pass(pass_an, pass_hien);
    }

    @FXML
    void handleRegister(ActionEvent event) {
        System.out.println("Đang chuyển sang trang Đăng ký...");
        switchScene(event, "/views/RegisterView.fxml", "Đăng ký tài khoản ");
    }

    @FXML
    void handleForgotPassword(ActionEvent event) {
        System.out.println("Mở form lấy lại mật khẩu...");
    }


}