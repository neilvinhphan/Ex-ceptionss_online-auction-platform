package org.example.frontend;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
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
        Text sceneTitle = new Text("TẠO TÀI KHOẢN");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 20));
        // Đặt ở cột 0, hàng 0, chiếm 2 cột
        sceneTitle.setFill(Color.WHITE);
        grid.add(sceneTitle, 0, 0, 2, 1);

        // Dòng 1: Họ và Tên (Dùng HBox để xếp ngang)
        Label hoten = new Label("Họ và tên: ");
        hoten.setStyle("-fx-text-fill: white");
        grid.add(hoten,0,1);
        //  grid.add(new Label("Họ và Tên:"), 0, 1);

        TextField ho = new TextField();
        ho.setPromptText("Họ");
        TextField ten = new TextField();
        ten.setPromptText("Tên");

        HBox hbName = new HBox(20); // Khoảng cách giữa ô Họ và Tên
        hbName.getChildren().addAll(ho, ten);
        grid.add(hbName, 1, 1);

        // Dòng 2: Username
        Label username = new Label("Tên đăng nhập:");
        username.setStyle("-fx-text-fill: white");
        grid.add(username, 0, 2);
        TextField tfusername = new TextField();
        grid.add(tfusername, 1, 2);

        // Dòng 3: Số điện thoại
        Label sdt = new Label("Số điện thoại: ");
        sdt.setStyle("-fx-text-fill: white");
        grid.add(sdt, 0, 3);
        TextField txtSdt = new TextField();
        grid.add(txtSdt, 1, 3);

        // Dòng 4: Email
        Label email = new Label("Email: ");
        email.setStyle("-fx-text-fill: white");
        grid.add(email, 0, 4);
        TextField txtEmail = new TextField();
        grid.add(txtEmail, 1, 4);

        // Dòng 5: Mật khẩu (Dùng PasswordField để hiện dấu *)
        Label pass = new Label("Mật khẩu: ");
        pass.setStyle("-fx-text-fill: white");
        grid.add(pass, 0, 5);

        PasswordField pass_an = new PasswordField();
        pass_an.setPromptText("Nhập mật khẩu");

        TextField pass_hien = new TextField();
        pass_hien.setVisible(false); // cho cút -> ẩn luôn khỏi cái grid
        pass_hien.setManaged(false); // đỡ tốn diện tích tính toán, ncl cái này với setVisible đi đôi !!!
        //Dong bo du lieu 2 o
        pass_hien.textProperty().bindBidirectional(pass_an.textProperty());
        //Tao nut xem
        Button eye = new Button("\uD83D\uDC40");
        eye.setStyle("-fx-background-color: transparent;"+
                "-fx-text-fill: black;"+
                "-fx-cursor:hand;"
        );
// 3. Xếp chồng dùng StackPane
        StackPane passwordContainer = new StackPane();
        passwordContainer.getChildren().addAll(pass_hien, pass_an, eye);

// Căn con mắt nằm bên phải của cái hộp
        StackPane.setAlignment(eye, Pos.CENTER_RIGHT);
        //     StackPane.setMargin(eye, new Insets(0, 10, 0, 0)); // Cách lề phải 10px cho đẹp

// Xử lý sự kiện nhấn nút
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
        grid.add(passwordContainer, 1, 5);

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
        HBox hb_dki = new HBox(dki);
        hb_dki.setAlignment(Pos.CENTER_RIGHT);
        //hb_dki.getChildren().add(dki);
        grid.add(hb_dki, 1, 6);

        // 5. Link chuyển sang Đăng nhập (cho đủ bộ Front-end)
        Hyperlink linkLogin = new Hyperlink("Đã có tài khoản? Đăng nhập");
        linkLogin.setStyle("-fx-text-fill: white");
        linkLogin.setOnAction(e->{
            Stage currentStage = (Stage) linkLogin.getScene().getWindow();
            currentStage.close();

            Signin signInUi= new Signin();
            Stage signInStage = new Stage();
            signInUi.start(signInStage);
        });
        grid.add(linkLogin, 1, 7);
        grid.setStyle("-fx-background-color: #447D9B;");
        // grid.setStyle("-fx-background-color: linear-gradient(to bottom right, #FAACBF, #FBC3C1);");
        // 6. Tạo Scene (Cảnh) và gắn vào Stage
        Scene scene = new Scene(grid, 900, 600);

        primaryStage.setScene(scene);

        // Hiển thị cửa sổ
        primaryStage.show();
    }

}
