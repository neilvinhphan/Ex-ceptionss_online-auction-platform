package org.example.frontend; // Thay đổi cho đúng package của bạn

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Chỉ đường dẫn đến file FXML của bạn
        // Lưu ý: Đường dẫn tính từ thư mục resources
        // Code chuẩn trong MainApp.java
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AntiqueView.fxml"));
        Parent root = loader.load();

        // 2. Thiết lập Scene (Màn hình)
        primaryStage.setTitle("Hệ thống Đấu giá");
        primaryStage.setScene(new Scene(root));

        // 3. Hiển thị cửa sổ
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}