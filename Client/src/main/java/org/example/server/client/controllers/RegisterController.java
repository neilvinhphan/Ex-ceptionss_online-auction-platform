package org.example.server.client.controllers;

import org.example.server.client.LoginView;
import org.example.server.client.RegisterView;

import javafx.scene.control.Alert;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RegisterController {
    private RegisterView view;

    public RegisterController(RegisterView view) {
        this.view = view;
        handleEvents();
    }

    private void handleEvents() {
        view.getBtnRegister().setOnAction(e -> {
            if (view.getTfUsername().getText().isEmpty() || view.getPassAn().getText().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Cảnh báo");
                alert.setHeaderText(null);
                alert.setContentText("Vui lòng điền đầy đủ thông tin trước khi đăng ký!");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thành công");
                alert.setHeaderText(null);
                alert.setContentText("Đăng ký tài khoản thành công! ");
                alert.showAndWait();

                Stage stage = (Stage) view.getRoot().getScene().getWindow();
                LoginView loginView = new LoginView();
                new org.example.server.client.controllers.LoginController(loginView); // Khởi tạo não cho trang đăng nhập
                stage.setScene(new Scene(loginView.getRoot(), 900, 650));
                stage.centerOnScreen();
            }
        });

        view.getLinkLogin().setOnAction(e -> {
            Stage stage = (Stage) view.getRoot().getScene().getWindow();
            LoginView lv = new LoginView();
            new org.example.server.client.controllers.LoginController(lv);
            stage.setScene(new Scene(lv.getRoot(), 900, 650));
        });
    }
}