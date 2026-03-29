package org.example.frontend;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class UserDashboard extends Application {

    // 1. KHUÔN MẪU SẢN PHẨM
    public static class Product {
        String name, price, image;

        public Product(String name, String price, String image) {
            this.name = name;
            this.price = price;
            this.image = image;
        }
    }

    public static ObservableList<Product> spDangDauGia = FXCollections.observableArrayList();
    public static ObservableList<Product> spDaDauGiaXong = FXCollections.observableArrayList();

    // HÀM VẼ THẺ SẢN PHẨM (Dùng chung cho cả 2 mục)
    private VBox createProductCard(Product p, boolean isOngoing) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        card.setPrefWidth(150);

        ImageView imageView = new ImageView();
        Image img = new Image(p.image, 130, 110, true,true); // rộng 130, cao 110; true: giữ tỷ lệ ; true: làm nét ảnh
        imageView.setImage(img);

        Label lblName = new Label(p.name);
        lblName.setStyle("-fx-background-color: white; -fx-text-fill: BLACK; -fx-font-weight: BOLD; -fx-font: Tahoma;-fx-font-size: 20;-fx-alignment: CENTER;");
        lblName.setWrapText(true);
        lblName.setPrefWidth(120); // Ép chiều rộng tối đa bằng đúng không gian thực tế của thẻ
        lblName.setMinHeight(Region.USE_PREF_SIZE); // Khuyến mãi thêm bùa chống "bóp chiều cao"

        Label lblPrice = new Label(p.price);
        lblPrice.setStyle("-fx-background-color: white; -fx-text-fill: BLACK; -fx-font-weight: BOLD; -fx-font: Tahoma;-fx-font-size: 20;-fx-alignment: CENTER;");
        lblPrice.setPrefWidth(130); // Ép chiều rộng tối đa bằng đúng không gian thực tế của thẻ
        lblPrice.setMinHeight(Region.USE_PREF_SIZE); // Khuyến mãi thêm bùa chống "bóp chiều cao"
        Button bt_trangthai = new Button(isOngoing ? "Đấu giá ngay" : "Đã kết thúc");
        if (isOngoing) {
            bt_trangthai.setStyle("-fx-background-color: #ff5722; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; ");
        } else {
            bt_trangthai.setStyle("-fx-background-color: #9e9e9e; -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 150;");
            bt_trangthai.setDisable(true); // Khóa không cho bấm
        }

        card.getChildren().addAll(imageView, lblName, lblPrice, bt_trangthai);
        return card;
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hệ thống Đấu giá");

        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #447D9B;");

        // --- THANH CÔNG CỤ ---
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_RIGHT);

        Button btCreateAuction = new Button("+ Tạo cuộc đấu giá");
        btCreateAuction.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btCreateAuction.setOnAction(e -> {
            Stage currentStage = (Stage) btCreateAuction.getScene().getWindow();
            currentStage.close();
            CreateAuction createAuction = new CreateAuction();
            Stage st_createAuction = new Stage();
            createAuction.start(st_createAuction);
        });
        Button btLogout = new Button("Đăng xuất");
        btLogout.setStyle("-fx-background-color: #f44336; -fx-text-fill: WHITE; -fx-font-weight: bold; -fx-cursor: hand;");
        btLogout.setOnAction(e -> {
            Stage currentStage = (Stage) btLogout.getScene().getWindow();
            currentStage.close();
            Signin signinUi = new Signin();
            Stage st_signin = new Stage();
            signinUi.start(st_signin);
        });
        topBar.getChildren().addAll(btCreateAuction, btLogout);

        Label lb_spDangDauGia = new Label("🔥 ĐANG TRONG QUÁ TRÌNH ĐẤU GIÁ");
        lb_spDangDauGia.setFont(Font.font("Tahoma", FontWeight.BOLD, 18));
        lb_spDangDauGia.setTextFill(Color.WHITE);

        FlowPane fl_spDangDauGia = new FlowPane();
        fl_spDangDauGia.setHgap(15);
        fl_spDangDauGia.setVgap(15);
        for (Product p : spDangDauGia) {
            fl_spDangDauGia.getChildren().add(createProductCard(p, true));
        }

        ScrollPane scroll_spDangDauGia = new ScrollPane(fl_spDangDauGia);
        scroll_spDangDauGia.setFitToWidth(true);
        scroll_spDangDauGia.setStyle("-fx-background: #447D9B; -fx-background-color: transparent;");
        scroll_spDangDauGia.setPrefHeight(300); // Chiều cao cố định để chừa chỗ cho mục dưới
        scroll_spDangDauGia.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);


        Label lb_spDaXong = new Label("✅ ĐÃ ĐẤU GIÁ XONG");
        lb_spDaXong.setFont(Font.font("Tahoma", FontWeight.BOLD, 18));
        lb_spDaXong.setTextFill(Color.WHITE);

        FlowPane fl_spDaXong = new FlowPane();
        fl_spDaXong.setHgap(15);
        fl_spDaXong.setVgap(15);
        for (Product p : spDaDauGiaXong) {
            fl_spDaXong.getChildren().add(createProductCard(p, false));
        }
        ScrollPane scroll_spDaXong = new ScrollPane(fl_spDaXong);
        scroll_spDaXong.setFitToWidth(true);
        scroll_spDaXong.setStyle("-fx-background: #447D9B; -fx-background-color: transparent;");
        scroll_spDaXong.setPrefHeight(300);
        scroll_spDaXong.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);


        mainLayout.getChildren().addAll(topBar, lb_spDangDauGia, scroll_spDangDauGia, lb_spDaXong, scroll_spDaXong);

        Scene scene = new Scene(mainLayout, 900, 650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


}