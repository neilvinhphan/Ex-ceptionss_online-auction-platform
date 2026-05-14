package org.example.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminSidebarController extends BaseController implements Initializable {

    @FXML private Button btnDashboard;
    @FXML private Button btnApprove;
    @FXML private Button btnUsers;
    @FXML private Button btnAuctions;
    @FXML private Button btnAlerts;

    // Biến static để ghi nhớ trang nào đang được mở
    private static String activePage = "DASHBOARD";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Khi Sidebar được load lên, kiểm tra biến static và tô màu nút tương ứng
        highlightActiveButton();
    }

    private void highlightActiveButton() {
        // Reset tất cả về màu mặc định (Trong suốt)
        resetStyles(btnDashboard, btnApprove, btnUsers, btnAuctions, btnAlerts);

        // Tô màu nút đang hoạt động (Màu xanh Indigo)
        switch (activePage) {
            case "DASHBOARD": applyActiveStyle(btnDashboard); break;
            case "APPROVE": applyActiveStyle(btnApprove); break;
            case "USERS": applyActiveStyle(btnUsers); break;
            case "AUCTIONS": applyActiveStyle(btnAuctions); break;
            case "ALERTS": applyActiveStyle(btnAlerts); break;
        }
    }

    // Các hàm xử lý sự kiện nút bấm
    @FXML
    private void handleDashboard(ActionEvent event) {
        activePage = "DASHBOARD";
        switchScene(event, "/views/AdminDashboardView.fxml", "Tổng quan hệ thống");
    }

    @FXML
    private void handleApproveItem(ActionEvent event) {
        activePage = "APPROVE";
        switchScene(event, "/views/ItemApprovalView.fxml", "Duyệt tài sản");
    }

    @FXML
    private void handleManageUsers(ActionEvent event) {
        activePage = "USERS";
        switchScene(event, "/views/ManageUserView.fxml", "Quản lý người dùng");
    }

    @FXML
    private void handleManageAuctions(ActionEvent event) {
        activePage = "AUCTIONS";
        switchScene(event, "/views/ManageAuctionView.fxml", "Quản lý phiên đấu giá");
    }


    // --- Helper Methods để xử lý Style ---
    private void resetStyles(Button... buttons) {
        for (Button btn : buttons) {
            // Kiểm tra xem nút có tồn tại không trước khi chọc vào nó
            if (btn != null) {
                btn.setStyle("-fx-font-weight: bold; -fx-background-color: transparent; " +
                        "-fx-text-fill: #555555; -fx-font-size: 15; -fx-padding: 15 20; -fx-cursor: hand;");
            } else {
                // Log ra để bạn biết chính xác nút nào đang bị lỗi fx:id
                System.out.println("CẢNH BÁO: Phát hiện một nút trong Sidebar bị NULL. Kiểm tra lại fx:id trong FXML!");
            }
        }
    }

    private void applyActiveStyle(Button btn) {
        btn.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e7ff; " +
                "-fx-text-fill: #4f46e5; -fx-font-size: 15; -fx-padding: 15 20; " +
                "-fx-cursor: hand; -fx-background-radius: 10;");
    }
}