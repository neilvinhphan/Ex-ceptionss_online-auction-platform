package org.example.frontend;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.example.frontend.controller.LoginController;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        LoginView loginView = new LoginView();
        new LoginController(loginView);
        Scene scene = new Scene(loginView.getRoot(), 900, 650);
        primaryStage.setTitle("Đấu giá ");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}