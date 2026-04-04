package org.example.server.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

public class LoginView {
    private GridPane root;
    private TextField tfUsername, passHien;
    private PasswordField passAn;
    private Button btnSignin, btnEye;
    private Hyperlink linkRegis;

    public LoginView() {
        initUI();
    }

    private void initUI() {
        root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.setHgap(10);
        root.setVgap(10);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #447D9B");

        Text title = new Text("ĐĂNG NHẬP");
        title.setFont(Font.font("Tahoma", FontWeight.BOLD, 25));
        title.setFill(Color.WHITE);
        root.add(title, 0, 0, 2, 1);

        root.add(new Label("Tên đăng nhập: ") {{
            setStyle("-fx-text-fill: white");
        }}, 0, 1);
        tfUsername = new TextField();
        root.add(tfUsername, 1, 1);

        root.add(new Label("Mật khẩu: ") {{
            setStyle("-fx-text-fill: white");
        }}, 0, 2);
        passAn = new PasswordField();
        passHien = new TextField();
        passHien.setVisible(false);
        passHien.setManaged(false);
        passHien.textProperty().bindBidirectional(passAn.textProperty());
        btnEye = new Button("\uD83D\uDC40");
        btnEye.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        StackPane passPane = new StackPane(passHien, passAn, btnEye);
        StackPane.setAlignment(btnEye, Pos.CENTER_RIGHT);
        root.add(passPane, 1, 2);

        btnSignin = new Button("Đăng nhập");
        btnSignin.setPrefWidth(150);
        btnSignin.setStyle("-fx-background-color: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10;");

        HBox hbBtn = new HBox(btnSignin);
        hbBtn.setAlignment(Pos.CENTER_RIGHT);
        root.add(hbBtn, 1, 3);

        linkRegis = new Hyperlink("Chưa có tài khoản? Đăng kí ngay");
        linkRegis.setStyle("-fx-text-fill: white");
        root.add(linkRegis, 1, 4);
    }

    public GridPane getRoot() {
        return root;
    }

    public TextField getTfUsername() {
        return tfUsername;
    }

    public PasswordField getPassAn() {
        return passAn;
    }

    public TextField getPassHien() {
        return passHien;
    }

    public Button getBtnSignin() {
        return btnSignin;
    }

    public Button getBtnEye() {
        return btnEye;
    }

    public Hyperlink getLinkRegis() {
        return linkRegis;
    }
}