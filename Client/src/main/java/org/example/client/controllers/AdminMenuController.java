package org.example.client.controllers;

import org.example.client.utils.UserSession;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class AdminMenuController extends BaseController {

    /**
     * Xử lý sự kiện khi Admin muốn xem giao diện của người dùng bình thường.
     * Lưu ý: Admin lúc này sẽ đóng vai trò như một khách vãng lai (Guest).
     */
    @FXML
    private void handleSwitchToUserUI(ActionEvent event) {
        System.out.println("Chuyển hướng sang giao diện người dùng...");
        switchScene(event, "/views/MainView.fxml", "Trang chủ - AuctionPro");
    }

    /**
     * Xử lý sự kiện đăng xuất khỏi hệ thống Admin.
     * Sẽ hiển thị một hộp thoại xác nhận trước khi thoát.
     */
    @FXML
    private void handleLogout(ActionEvent event) {
        // Tạo hộp thoại xác nhận
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận đăng xuất");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất khỏi Admin Panel?");

        // Lấy kết quả người dùng bấm (OK hoặc Cancel)
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("Admin đã đăng xuất.");
            UserSession.getInstance().cleanUserSession();
            switchScene(event, "/views/LoginView.fxml", "Đăng nhập ");
        }
    }
}