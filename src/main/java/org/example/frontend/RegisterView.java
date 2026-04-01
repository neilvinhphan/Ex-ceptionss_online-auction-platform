package org.example.frontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

public class RegisterView {
    private GridPane root;
    private TextField tfHo, tfDem, tfTen, tfUsername, tfSdt, tfEmail, passHien;
    private PasswordField passAn;
    private Button btnEye1, btnRegister;
    private Hyperlink linkLogin;

    public RegisterView() {
        initUI();
    }

    private void initUI() {
        root = new GridPane();
        root.setAlignment(Pos.CENTER);
        root.setHgap(10); root.setVgap(18);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #447D9B;");

        Text sceneTitle = new Text("ĐĂNG KÍ TÀI KHOẢN");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 20));
        sceneTitle.setFill(Color.WHITE);
        root.add(sceneTitle, 0, 0, 2, 1);

        tfHo = new TextField(); tfHo.setPromptText("Họ");
        tfDem = new TextField(); tfDem.setPromptText("Tên đệm");
        tfTen = new TextField(); tfTen.setPromptText("Tên");
        HBox hbName = new HBox(15, tfHo, tfDem, tfTen);
        root.add(hbName, 0, 1);

        tfUsername = new TextField(); tfUsername.setPromptText("Tên đăng nhập");
        root.add(tfUsername, 0, 2);

        tfSdt = new TextField(); tfSdt.setPromptText("Số điện thoại");
        root.add(tfSdt, 0, 3);

        tfEmail = new TextField(); tfEmail.setPromptText("Email");
        root.add(tfEmail, 0, 4);

        passAn = new PasswordField(); passAn.setPromptText("Nhập mật khẩu");
        passHien = new TextField(); passHien.setVisible(false); passHien.setManaged(false);
        passHien.textProperty().bindBidirectional(passAn.textProperty());
        btnEye1 = new Button("\uD83D\uDC40");
        btnEye1.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        StackPane pane1 = new StackPane(passHien, passAn, btnEye1);
        StackPane.setAlignment(btnEye1, Pos.CENTER_RIGHT);
        root.add(pane1, 0, 5);

        btnRegister = new Button("Đăng ký ngay");
        btnRegister.setStyle("-fx-background-color: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20;");
        root.add(btnRegister, 0, 7);

        linkLogin = new Hyperlink("Đã có tài khoản? Đăng nhập");
        linkLogin.setStyle("-fx-text-fill: white");
        root.add(linkLogin, 0, 8);
    }


    public GridPane getRoot() { return root; }

    public Button getBtnRegister() { return btnRegister; }

    public Hyperlink getLinkLogin() { return linkLogin; }

    public Button getBtnEye1() { return btnEye1; }

    public PasswordField getPassAn() { return passAn; }

    public TextField getPassHien() { return passHien; }
    public TextField getTfUsername() {
        return tfUsername;
    }
    public TextField getTfHo() { return tfHo; }
    public TextField getTfDem() { return tfDem; }
    public TextField getTfTen() { return tfTen; }
    public TextField getTfSdt() { return tfSdt; }
    public TextField getTfEmail() { return tfEmail; }
}