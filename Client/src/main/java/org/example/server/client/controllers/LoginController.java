package org.example.server.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField tfuserName;
    @FXML
    private PasswordField pass_an;
    @FXML
    private TextField pass_hien;
    private boolean isPasswordVisible = false;
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
    }

    @FXML
    void hienthi_pass(ActionEvent event) {
        if (!isPasswordVisible) {
            pass_hien.setText(pass_an.getText());
            pass_hien.setVisible(true);
            pass_an.setVisible(false);
            isPasswordVisible = true;
        } else {
            pass_an.setText(pass_hien.getText());
            pass_an.setVisible(true);
            pass_hien.setVisible(false);
            isPasswordVisible = false;
        }
    }
    @FXML
    void handleRegister(ActionEvent event) {
        System.out.println("Đang chuyển sang trang Đăng ký...");
    }

    @FXML
    void handleForgotPassword(ActionEvent event) {
        System.out.println("Mở form lấy lại mật khẩu...");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}