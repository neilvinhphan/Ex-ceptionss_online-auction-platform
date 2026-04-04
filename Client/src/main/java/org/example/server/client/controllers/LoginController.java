package org.example.server.client.controllers;

import org.example.server.client.LoginView;
import org.example.server.client.MainView;
import org.example.server.client.RegisterView;

import javafx.scene.control.Alert;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginController {
    private LoginView view;

    public LoginController(LoginView view) {
        this.view = view;
        initEvents();
    }

    private void initEvents() {
        view.getBtnSignin().setOnAction(e -> {
            String user = view.getTfUsername().getText();
            String pass = view.getPassAn().getText();

            if (user.isEmpty() || pass.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Thông báo");
                alert.setHeaderText(null);
                alert.setContentText("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
                alert.showAndWait();
            } else {
                Stage stage = (Stage) view.getRoot().getScene().getWindow();
                MainView mv = new MainView();
                new MainController(mv);
                stage.setScene(new Scene(mv.getRoot(), 1200, 750));
                stage.centerOnScreen();
            }
        });

        view.getLinkRegis().setOnAction(e -> {
            Stage stage = (Stage) view.getRoot().getScene().getWindow();
            RegisterView rv = new RegisterView();
            new RegisterController(rv);
            stage.setScene(new Scene(rv.getRoot(), 900, 650));
        });

        view.getBtnEye().setOnAction(e -> {
            boolean isVisible = view.getPassAn().isVisible();
            view.getPassAn().setVisible(!isVisible);
            view.getPassAn().setManaged(!isVisible);
            view.getPassHien().setVisible(isVisible);
            view.getPassHien().setManaged(isVisible);
        });
    }
}