package org.example.frontend.controller;
import org.example.frontend.*;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainController {
    private MainView view;

    public MainController(MainView view) {
        this.view = view;
        initEvents();
    }

    private void initEvents() {
        // 1. Xử lý nút Đăng nhập trên Header (Giữ nguyên)
        view.getBtnLogin().setOnAction(e -> {
            Stage stage = (Stage) view.getRoot().getScene().getWindow();
            LoginView lv = new LoginView();
            new LoginController(lv);
            stage.setScene(new Scene(lv.getRoot(), 900, 650));
        });

        // 2. Xử lý nút Đăng ký trên Header (Giữ nguyên)
        view.getBtnRegister().setOnAction(e -> {
            Stage stage = (Stage) view.getRoot().getScene().getWindow();
            RegisterView rv = new RegisterView();
            new RegisterController(rv);
            stage.setScene(new Scene(rv.getRoot(), 900, 650));
        });

        // 3. Xử lý nút TÌM KIẾM (MỚI THÊM)
        view.getBtnSearch().setOnAction(e -> {
            // Lấy loại hàng đang được chọn trong ô ComboBox sổ xuống
            String selectedCategory = view.getCategoryBox().getSelectionModel().getSelectedItem();
            System.out.println(">>> Đang tìm kiếm sản phẩm thuộc danh mục: " + selectedCategory);
            // Sau này ông viết logic gọi Backend để tìm kiếm ở đây nhé
        });

        // 4. Xử lý hiệu ứng cho các danh mục hình tròn (Giữ nguyên)
        view.getCategoryNodes().forEach((name, node) -> {
            // lookup(".stack-pane") để tìm cái hình tròn bên trong VBox
            node.setOnMouseEntered(e -> node.lookup(".stack-pane").setStyle("-fx-background-color: #d1f0db; -fx-background-radius: 50%; -fx-min-width: 90; -fx-min-height: 90;"));
            node.setOnMouseExited(e -> node.lookup(".stack-pane").setStyle("-fx-background-color: #e8f9ec; -fx-background-radius: 50%; -fx-min-width: 90; -fx-min-height: 90;"));
        });
    }
}