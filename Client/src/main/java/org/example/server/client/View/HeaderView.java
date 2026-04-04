package org.example.server.client.View;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

public class HeaderView extends HBox {
    private Button btnRegister, btnLogin, btnHome;

    public HeaderView() {
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(10, 25, 10, 25));
        this.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-background-radius: 30;");
        this.setMaxWidth(1150); // Để thanh header không tràn quá sát mép

        Label logo = new Label("ĐẤU GIÁ");
        logo.setTextFill(Color.web("#218c74"));
        logo.setFont(Font.font("System", FontWeight.BOLD, 18));
        logo.setCursor(javafx.scene.Cursor.HAND);

        HBox navBox = new HBox(20);
        navBox.setAlignment(Pos.CENTER);
        navBox.setPadding(new Insets(0, 0, 0, 40));

        btnHome = createNavBtn("Trang chủ");
        navBox.getChildren().addAll(btnHome, createNavBtn("Danh mục"), createNavBtn("Phòng đấu giá"), createNavBtn("Giới thiệu"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnRegister = new Button("Đăng ký");
        btnRegister.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-weight: bold;");

        btnLogin = new Button("Đăng nhập");
        btnLogin.setStyle("-fx-background-color: #218c74; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-weight: bold;");

        this.getChildren().addAll(logo, navBox, spacer, new HBox(15, btnRegister, btnLogin));
    }

    private Button createNavBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        return b;
    }

    public Button getBtnRegister() { return btnRegister; }
    public Button getBtnLogin() { return btnLogin; }
    public Button getBtnHome() { return btnHome; }
}