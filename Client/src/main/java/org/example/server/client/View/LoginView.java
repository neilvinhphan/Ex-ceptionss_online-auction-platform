package org.example.server.client.View;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

public class LoginView {
    private VBox root;
    private HeaderView header;
    private TextField tfUsername;
    private PasswordField passAn;
    private Button btnSignin;
    private Hyperlink linkRegis;

    public LoginView() {
        header = new HeaderView();

        GridPane form = new GridPane();
        form.setAlignment(Pos.CENTER);
        form.setHgap(15); form.setVgap(15);
        form.setPadding(new Insets(50));
        form.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        form.setMaxSize(450, 400);

        Text title = new Text("ĐĂNG NHẬP");
        title.setFont(Font.font("System", FontWeight.BOLD, 25));
        title.setFill(Color.web("#218c74")); // Đổi sang màu xanh MainView
        form.add(title, 0, 0, 2, 1);

        form.add(new Label("Tên đăng nhập:"), 0, 1);
        tfUsername = new TextField();
        form.add(tfUsername, 1, 1);

        form.add(new Label("Mật khẩu:"), 0, 2);
        passAn = new PasswordField();
        form.add(passAn, 1, 2);

        btnSignin = new Button("Đăng nhập");
        btnSignin.setStyle("-fx-background-color: #218c74; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 30;");
        form.add(btnSignin, 1, 3);

        linkRegis = new Hyperlink("Chưa có tài khoản? Đăng kí ngay");
        form.add(linkRegis, 1, 4);

        // Gộp Header và Form
        root = new VBox(50, header, form);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f4f4f4;"); // Màu nền trung tính
    }

    public VBox getRoot() { return root; }
    public HeaderView getHeader() { return header; }
    public Button getBtnSignin() { return btnSignin; }
    public TextField getTfUsername() { return tfUsername; }
    public PasswordField getPassAn() { return passAn; }
    public Hyperlink getLinkRegis() { return linkRegis; }
}