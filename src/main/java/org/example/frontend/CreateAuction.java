package org.example.frontend;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class CreateAuction extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hệ thống Đấu giá - Thêm sản phẩm");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(25));
        grid.setStyle("-fx-background-color: #447D9B;");

        Text sceneTitle = new Text("TẠO CUỘC ĐẤU GIÁ MỚI");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 20));
        sceneTitle.setFill(Color.WHITE);
        GridPane.setHalignment(sceneTitle, javafx.geometry.HPos.CENTER);
        grid.add(sceneTitle, 0, 0, 2, 1);

        Label lblName = new Label("Tên sản phẩm:");
        lblName.setTextFill(Color.WHITE);
        grid.add(lblName, 0, 1);
        TextField txtName = new TextField();
        grid.add(txtName, 1, 1);

        Label lblPrice = new Label("Giá khởi điểm (VNĐ):");
        lblPrice.setTextFill(Color.WHITE);
        grid.add(lblPrice, 0, 2);
        TextField txtPrice = new TextField();
        grid.add(txtPrice, 1, 2);

        Label lblDesc = new Label("Mô tả sản phẩm:");
        lblDesc.setTextFill(Color.WHITE);
        grid.add(lblDesc, 0, 3);
        TextArea txtDesc = new TextArea();
        txtDesc.setPrefRowCount(4);
        txtDesc.setWrapText(true);
        grid.add(txtDesc, 1, 3);

        // -- NÚT CHỌN ẢNH (CHỈ LÀM GIAO DIỆN FRONT-END) --
        Label lblImage = new Label("Hình ảnh:");
        lblImage.setTextFill(Color.WHITE);
        grid.add(lblImage, 0, 4);

        Button btnUpload = new Button("Chọn ảnh từ máy...");
        Label lblFileName = new Label("Chưa có file nào được chọn.");
        lblFileName.setTextFill(Color.LIGHTGRAY);
        HBox hbUpload = new HBox(10, btnUpload, lblFileName);
        hbUpload.setAlignment(Pos.CENTER_LEFT);

        btnUpload.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chọn hình ảnh sản phẩm");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );

            // Mở cửa sổ chọn file
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                // Front-end chỉ có trách nhiệm báo lên màn hình là "Đã chọn file này"
                lblFileName.setText(selectedFile.getName());
            }
        });
        grid.add(hbUpload, 1, 4);

        // NÚT ĐĂNG SẢN PHẨM
        Button btnSubmit = new Button("Đăng sản phẩm");
        btnSubmit.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 20;");

        Button btnBack = new Button("Quay lại");
        btnBack.setStyle("-fx-background-color: transparent; -fx-border-color: white; -fx-border-radius: 3; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 9 19;");

        btnSubmit.setOnAction(e -> {
            String name = txtName.getText().trim();
            String price = txtPrice.getText().trim();

            if (name.isEmpty() || price.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("Vui lòng nhập Tên sản phẩm và Giá!");
                alert.showAndWait();
                return;
            }

            // Đẩy sản phẩm sang Dashboard kèm một link ảnh mặc định (Vì không có Backend trả link)
            String dummyImageUrl = "https://cdn-icons-png.flaticon.com/512/3412/3412862.png";
            UserDashboard.spDangDauGia.add(new UserDashboard.Product(name, price, dummyImageUrl));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Sản phẩm đã được đẩy lên Sàn đấu giá!");
            alert.showAndWait();

            btnBack.fire(); // Tự động click quay lại Dashboard
        });

        btnBack.setOnAction(e -> {
            Stage currentStage = (Stage) btnBack.getScene().getWindow();
            currentStage.close();
            try {
                new UserDashboard().start(new Stage());
            } catch (Exception ex) {
            }
        });

        HBox hbButtons = new HBox(15, btnSubmit, btnBack);
        hbButtons.setAlignment(Pos.CENTER_RIGHT);
        grid.add(hbButtons, 1, 5);

        Scene scene = new Scene(grid, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}