package org.example.frontend;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Register extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Stage là cái cửa sổ chương trình
        primaryStage.setTitle("Hệ thống Đấu giá - Đăng ký");

        // 2. GridPane là một cái lưới để ông đặt các ô vào (giống chia cột trong Excel)
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER); // Căn giữa toàn bộ lưới
        grid.setHgap(10); // Khoảng cách giữa các cột
        grid.setVgap(18); // Khoảng cách giữa các hàng
        grid.setPadding(new Insets(25, 25, 25, 25)); // Lề xung quanh lưới
        // 3. Tiêu đề trang
        Text sceneTitle = new Text("ĐĂNG KÍ TÀI KHOẢN");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 20));
        // Đặt ở cột 0, hàng 0, chiếm 2 cột
        sceneTitle.setFill(Color.WHITE);
        grid.add(sceneTitle, 0, 0, 2, 1);

        // Dòng 1: Họ và Tên (Dùng HBox để xếp ngang)
       /* Label hoten = new Label("Họ và tên: ");
        hoten.setStyle("-fx-text-fill: white");
        grid.add(hoten, 0, 1);
          grid.add(new Label("Họ và Tên:"), 0, 1); */

        TextField tfHo = new TextField();
        tfHo.setPromptText("Họ");
        TextField tfDem = new TextField();
        tfDem.setPromptText("Têm đệm");
        TextField tfTen = new TextField();
        tfTen.setPromptText("Tên");

        HBox hbName = new HBox(15); // Khoảng cách giữa ô Họ và Tên
        hbName.getChildren().addAll(tfHo, tfDem,tfTen);
        grid.add(hbName, 0, 1);

        // Dòng 2: Username
        TextField tfusername = new TextField();
        tfusername.setPromptText("Tên đăng nhập");
        grid.add(tfusername, 0, 2);

        // Dòng 3: Số điện thoại
        TextField tfSdt = new TextField();
        tfSdt.setPromptText("Số điện thoại");
        grid.add(tfSdt, 0, 3);

        // Dòng 4: Email
        TextField tfEmail = new TextField();
        tfEmail.setPromptText("Email");
        grid.add(tfEmail, 0, 4);

        PasswordField pass_an = new PasswordField();
        pass_an.setPromptText("Nhập mật khẩu");
        TextField pass_hien = new TextField();
        pass_hien.setVisible(false); // cho cút -> ẩn luôn khỏi cái grid
        pass_hien.setManaged(false); // đỡ tốn diện tích tính toán, ncl cái này với setVisible đi đôi !!!
        pass_hien.textProperty().bindBidirectional(pass_an.textProperty()); //Dong bo du lieu 2 o
        Button eye = new Button("\uD83D\uDC40");
        eye.setStyle("-fx-background-color: transparent;" +
                "-fx-text-fill: black;" +
                "-fx-cursor:hand;"
        );
        StackPane passwordContainer = new StackPane();
        passwordContainer.getChildren().addAll(pass_hien, pass_an, eye);
        StackPane.setAlignment(eye, Pos.CENTER_RIGHT);
        eye.setOnAction(e -> {
            if (pass_an.isVisible()) {
                pass_an.setVisible(false);
                pass_an.setManaged(false);
                pass_hien.setVisible(true);
                pass_hien.setManaged(true);
            } else {
                pass_hien.setVisible(false);
                pass_hien.setManaged(false);
                pass_an.setVisible(true);
                pass_an.setManaged(true);
            }
        });
        grid.add(passwordContainer, 0, 5);


        //
        PasswordField pass_an1 = new PasswordField();
        pass_an1.setPromptText("Nhập lại mật khẩu");
        TextField pass_hien1 = new TextField();
        pass_hien1.setVisible(false); // cho cút -> ẩn luôn khỏi cái grid
        pass_hien1.setManaged(false); // đỡ tốn diện tích tính toán, ncl cái này với setVisible đi đôi !!!
        pass_hien1.textProperty().bindBidirectional(pass_an1.textProperty()); //Dong bo du lieu 2 o
        Button eye1 = new Button("\uD83D\uDC40");
        eye1.setStyle("-fx-background-color: transparent;" +
                "-fx-text-fill: black;" +
                "-fx-cursor:hand;"
        );
        StackPane passwordContainer1 = new StackPane();
        passwordContainer1.getChildren().addAll(pass_hien1, pass_an1, eye1);
        StackPane.setAlignment(eye1, Pos.CENTER_RIGHT);
        eye1.setOnAction(e -> {
            if (pass_an1.isVisible()) {
                pass_an1.setVisible(false);
                pass_an1.setManaged(false);
                pass_hien1.setVisible(true);
                pass_hien1.setManaged(true);
            } else {
                pass_hien1.setVisible(false);
                pass_hien1.setManaged(false);
                pass_an1.setVisible(true);
                pass_an1.setManaged(true);
            }
        });
        grid.add(passwordContainer1, 0, 6);
        // 4. Nút bấm Đăng ký
        Button dki = new Button("Đăng ký ngay");
        dki.setPrefWidth(200);

        dki.setStyle(
                "-fx-background-color: white; " +
                        "-fx-text-fill: black; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 10; " + // bo góc
                        "-fx-padding: 10 20 10 20;" // trên-phải-dưới-trái: KC từ viền đến chữ đăng ký ngay"
        );
        dki.setOnAction(e -> {
            if (tfHo.getText().trim().isEmpty() ||
                    tfTen.getText().trim().isEmpty() ||
                    tfusername.getText().trim().isEmpty() ||
                    tfSdt.getText().trim().isEmpty() ||
                    tfEmail.getText().trim().isEmpty() ||
                    pass_hien.getText().trim().isEmpty() ||
                    pass_an.getText().trim().isEmpty()) {

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Cảnh báo");
                alert.setHeaderText(null); // Để null cho gọn, không bị hiện 2 dòng tiêu đề
                alert.setContentText("Vui lòng nhập đầy đủ thông tin trước khi đăng ký!");
                alert.showAndWait(); // Lệnh này làm cái hộp thoại hiện lên và bắt người dùng phải bấm OK mới được làm tiếp

            } else {
                Stage currentStage = (Stage) dki.getScene().getWindow();
                currentStage.close();
                Login signin = new Login();
                Stage st_signin = new Stage();
                signin.start(st_signin);
            }
        });
        HBox hb_dki = new HBox(dki);
        hb_dki.setAlignment(Pos.CENTER_RIGHT);
        grid.add(hb_dki, 0, 7);
        Hyperlink linkLogin = new Hyperlink("Đã có tài khoản? Đăng nhập");
        linkLogin.setStyle("-fx-text-fill: white");
        linkLogin.setOnAction(e -> {
            Stage currentStage = (Stage) linkLogin.getScene().getWindow();
            currentStage.close();

            Login signInUi = new Login();
            Stage signInStage = new Stage();
            signInUi.start(signInStage);
        });
        grid.add(linkLogin, 0, 8);
        grid.setStyle("-fx-background-color: #447D9B;");
        Scene scene = new Scene(grid, 900, 650);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

}
