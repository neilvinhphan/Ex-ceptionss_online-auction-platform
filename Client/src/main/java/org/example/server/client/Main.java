package org.example.server.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.example.server.client.View.LoginView;
import org.example.server.client.controllers.LoginController;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        LoginView loginView = new LoginView();
        new LoginController(loginView);
        Scene scene = new Scene(loginView.getRoot(), 900, 650);
        primaryStage.setTitle("Đấu giá - Đăng nhập");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void main(String[] args) { launch(args); }
}