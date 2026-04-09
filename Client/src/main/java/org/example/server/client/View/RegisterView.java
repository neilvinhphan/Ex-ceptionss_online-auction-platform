package org.example.server.client.View; // Ông nhớ check lại tên package cho chuẩn nhé

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

public class RegisterView {
    private VBox root; // Đổi từ GridPane sang VBox để chứa Header + Form
    private HeaderView header; // Thêm Header ở đây
    private TextField tfHo, tfDem, tfTen, tfUsername, tfSdt, tfEmail, passHien;
    private PasswordField passAn;
    private Button btnEye1, btnRegister;
    private Hyperlink linkLogin;

    public RegisterView() {
        initUI();
    }

    private void initUI() {
        // 1. Khởi tạo Header
        header = new HeaderView();

        // 2. Tạo Form Đăng ký (Dùng lại GridPane cũ của ông nhưng chỉnh lại màu)
        GridPane form = new GridPane();
        form.setAlignment(Pos.CENTER);
        form.setHgap(10);
        form.setVgap(18);
        form.setPadding(new Insets(30));
        // Đổi màu nền trắng, bo góc để trông hiện đại hơn trên nền xám
        form.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        form.setMaxWidth(600);

        Text sceneTitle = new Text("ĐĂNG KÍ TÀI KHOẢN");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 22));
        sceneTitle.setFill(Color.web("#218c74")); // Dùng màu xanh của MainView
        form.add(sceneTitle, 0, 0, 2, 1);

        // Các trường nhập liệu (Giữ nguyên logic của ông)
        tfHo = new TextField(); tfHo.setPromptText("Họ");
        tfDem = new TextField(); tfDem.setPromptText("Tên đệm");
        tfTen = new TextField(); tfTen.setPromptText("Tên");
        HBox hbName = new HBox(15, tfHo, tfDem, tfTen);
        form.add(hbName, 0, 1);

        tfUsername = new TextField(); tfUsername.setPromptText("Tên đăng nhập");
        form.add(tfUsername, 0, 2);

        tfSdt = new TextField(); tfSdt.setPromptText("Số điện thoại");
        form.add(tfSdt, 0, 3);

        tfEmail = new TextField(); tfEmail.setPromptText("Email");
        form.add(tfEmail, 0, 4);

        // Mật khẩu có mắt ẩn hiện
        passAn = new PasswordField(); passAn.setPromptText("Nhập mật khẩu");
        passHien = new TextField();
        passHien.setVisible(false); passHien.setManaged(false);
        passHien.textProperty().bindBidirectional(passAn.textProperty());
        btnEye1 = new Button("\uD83D\uDC40");
        btnEye1.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        StackPane pane1 = new StackPane(passHien, passAn, btnEye1);
        StackPane.setAlignment(btnEye1, Pos.CENTER_RIGHT);
        form.add(pane1, 0, 5);

        btnRegister = new Button("Đăng ký ngay");
        // Đổi nút sang màu xanh lục cho đồng bộ
        btnRegister.setStyle("-fx-background-color: #218c74; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand;");
        form.add(btnRegister, 0, 7);

        linkLogin = new Hyperlink("Đã có tài khoản? Đăng nhập ngay");
        linkLogin.setStyle("-fx-text-fill: #218c74; -fx-font-weight: bold;");
        form.add(linkLogin, 0, 8);

        // 3. Gộp Header và Form vào Root
        root = new VBox(40); // Khoảng cách giữa Header và Form là 40
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20));
        // Màu nền xám nhạt để làm nổi bật cái Form trắng
        root.setStyle("-fx-background-color: #f4f4f4;");

        root.getChildren().addAll(header, form);
    }

    // --- GETTERS (Để Controller hoạt động) ---
    public VBox getRoot() { return root; }
    public HeaderView getHeader() { return header; }
    public Button getBtnRegister() { return btnRegister; }
    public Hyperlink getLinkLogin() { return linkLogin; }
    public Button getBtnEye1() { return btnEye1; }
    public PasswordField getPassAn() { return passAn; }
    public TextField getPassHien() { return passHien; }
    public TextField getTfUsername() { return tfUsername; }
    public TextField getTfEmail() { return tfEmail; }
    // Ông có thể thêm các getter cho Sdt, Ho, Ten nếu cần lấy dữ liệu đăng ký
}