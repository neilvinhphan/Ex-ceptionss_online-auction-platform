package org.example.frontend;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

import javafx.application.Application;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class Signin extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hệ thống Đấu giá - Đăng nhập");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25));

        Text sceneTitle = new Text("ĐĂNG NHẬP");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 15));
        sceneTitle.setFill(Color.WHITE);
        grid.add(sceneTitle, 0, 0, 2, 1);

        Label username = new Label("Tên đăng nhập: ");
        username.setStyle("-fx-text-fill: white");
        grid.add(username, 0, 1);
        TextField tfusename = new TextField();
        grid.add(tfusename, 1, 1);

        Label pass = new Label("Mật khẩu: ");
        pass.setStyle("-fx-text-fill: white");
        grid.add(pass, 0, 2);
        PasswordField pass_an = new PasswordField();
        TextField pass_hien = new TextField();
        pass_hien.textProperty().bindBidirectional(pass_an.textProperty());
        Button eye = new Button("\uD83D\uDC40");
        eye.setStyle("-fx-background-color: transparent;"+
                " -fx-text-fill: black; "+
                "-fx-cursor: hand;");

        pass_hien.setVisible(false);
        pass_hien.setManaged(false);
        StackPane passwordContainer = new StackPane(pass_hien, pass_an, eye);
        StackPane.setAlignment(eye, Pos.CENTER_RIGHT);
        eye.setOnAction(e -> {
            if (pass_an.isVisible()) {
                pass_an.setVisible(false);
                pass_an.setManaged(false);
                pass_hien.setVisible(true);
                pass_hien.setManaged(true);
            } else {
                pass_an.setVisible(true);
                pass_an.setManaged(true);
                pass_hien.setVisible(false);
                pass_hien.setManaged(false);
            }
        });
        grid.add(passwordContainer, 1, 2);

        Button signin = new Button("Đăng nhập");
        signin.setPrefWidth(150);
        signin.setStyle("-fx-background-color: white;" + "-fx-text-fill: black;" + "-fx-font-weight: bold;" + "-fx-background-radius: 10;" + "-fx-padding: 10 20 10 20;");
        HBox hb_signin = new HBox(signin);
        hb_signin.setAlignment(Pos.CENTER_RIGHT);
        grid.add(hb_signin, 1, 3);

        Hyperlink linkRegis = new Hyperlink("Chưa có tài khoản? Đăng kí ngay");
        grid.add(linkRegis, 1, 4);
        linkRegis.setStyle("-fx-text-fill: white");
        linkRegis.setOnAction(e -> {
            Stage currentStage = (Stage) linkRegis.getScene().getWindow();
            currentStage.close();
            Register registerUi = new Register();
            Stage regisStage = new Stage();
            registerUi.start(regisStage);
        });

        grid.setStyle("-fx-background-color: #447D9B");
        Scene scene = new Scene(grid,900,600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
