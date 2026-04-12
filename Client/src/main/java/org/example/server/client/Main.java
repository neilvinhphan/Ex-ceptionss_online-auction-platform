package org.example.server.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Chỉ đường dẫn đến file FXML của bạn
        // Lưu ý: Đường dẫn phải khớp với cấu trúc trong thư mục resources
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
        Parent root = loader.load();

        // 2. Tạo Scene (màn hình) với file FXML đã load
        Scene scene = new Scene(root);

        // 3. Thiết lập Stage (Cửa sổ ứng dụng)
        primaryStage.setTitle("Hệ thống Đấu giá VNA");
        primaryStage.setScene(scene);

        // Để nó full màn hình ngay từ đầu như bạn muốn:
        primaryStage.setMaximized(true);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args); // Lệnh này kích hoạt ứng dụng JavaFX
    }
}