package org.example.server.client.controllers;

import org.example.server.client.View.*;
import javafx.scene.control.Alert;

public class LoginController {
    private LoginView view;

    public LoginController(LoginView view) {
        this.view = view;
        // Gán sự kiện cho Header của trang Login
        MainController.initCommonEvents(view.getHeader());

        view.getBtnSignin().setOnAction(e -> {
            if (view.getTfUsername().getText().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Nhập tài khoản!").show();
            } else {
                System.out.println("Đăng nhập thành công");
            }
        });

        view.getLinkRegis().setOnAction(e -> view.getHeader().getBtnRegister().fire());
    }
}