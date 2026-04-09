package org.example.server.client.controllers;

import org.example.server.client.View.RegisterView;

import org.example.server.client.View.LoginView;
import javafx.scene.control.Alert;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RegisterController {
    private RegisterView view;

    public RegisterController(RegisterView view) {
        this.view = view;
        // Kích hoạt sự kiện cho thanh Header (Đăng nhập, Trang chủ...)
        MainController.initCommonEvents(view.getHeader());
        handleEvents();
    }

    private void handleEvents() {
        // Xử lý nút Đăng ký
        view.getBtnRegister().setOnAction(e -> {
            // Lấy dữ liệu từ các ô nhập (Cần RegisterView có các hàm get tương ứng)
            String user = view.getTfUsername().getText();
            String pass = view.getPassAn().getText();
            String email = view.getTfEmail().getText();

            if (user.isEmpty() || pass.isEmpty() || email.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Cảnh báo");
                alert.setHeaderText(null);
                alert.setContentText("Vui lòng điền đầy đủ thông tin để đăng ký!");
                alert.showAndWait();
            } else {
                System.out.println("Đang đăng ký tài khoản: " + user);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thành công");
                alert.setContentText("Đăng ký thành công! Quay lại trang Đăng nhập.");
                alert.showAndWait();

                // Chuyển sang trang Login
                switchToLogin();
            }
        });

        // Xử lý Link chuyển nhanh sang Đăng nhập ở dưới form
        view.getLinkLogin().setOnAction(e -> switchToLogin());
    }

    private void switchToLogin() {
        Stage stage = (Stage) view.getRoot().getScene().getWindow();
        LoginView lv = new LoginView();
        new LoginController(lv);
        stage.setScene(new Scene(lv.getRoot(), 1200, 750));
    }
}