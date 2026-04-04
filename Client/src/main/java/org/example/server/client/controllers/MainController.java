package org.example.server.client.controllers;

import org.example.server.client.View.HeaderView;
import org.example.server.client.View.LoginView;
import org.example.server.client.View.MainView;
import org.example.server.client.View.RegisterView;
import org.example.server.client.View.*;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainController {
    private MainView view;

    public MainController(MainView view) {
        this.view = view;
        initCommonEvents(view.getHeader());

        view.getBtnSearch().setOnAction(e -> {
            System.out.println("Tìm kiếm: " + view.getCategoryBox().getValue());
        });
    }

    // Hàm này dùng chung để điều hướng từ Header
    public static void initCommonEvents(HeaderView header) {
        header.getBtnLogin().setOnAction(e -> {
            Stage stage = (Stage) header.getScene().getWindow();
            LoginView lv = new LoginView();
            new LoginController(lv);
            stage.setScene(new Scene(lv.getRoot(), 1200, 750));
        });

        header.getBtnRegister().setOnAction(e -> {
            Stage stage = (Stage) header.getScene().getWindow();
            RegisterView rv = new RegisterView();
            new RegisterController(rv);
            stage.setScene(new Scene(rv.getRoot(), 1200, 750));
        });

        header.getBtnHome().setOnAction(e -> {
            Stage stage = (Stage) header.getScene().getWindow();
            MainView mv = new MainView();
            new MainController(mv);
            stage.setScene(new Scene(mv.getRoot(), 1200, 750));
        });
    }
}