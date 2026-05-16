package org.example.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminSidebarController extends BaseController implements Initializable {

    @FXML private Button btnDashboard;
    @FXML private Button btnApprove;
    @FXML private Button btnUsers;
    @FXML private Button btnAuctions;
    @FXML private Button btnAlerts;

    // ÉP ĐÚNG TÊN FILE FXML ĐỂ LÀM GIÁ TRỊ MẶC ĐỊNH BAN ĐẦU
    public static String activePage = "AdminDashboardView.fxml";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        highlightActiveButton();
    }

    private void highlightActiveButton() {
        // 1. Reset tất cả các nút về trạng thái bình thường bằng setStyle cứng
        for(Node node : new Node[]{btnDashboard, btnApprove, btnUsers, btnAuctions, btnAlerts}){
            if(node != null ){
                node.setStyle("-fx-background-color: transparent; -fx-text-fill: #555555; -fx-font-weight: bold; -fx-font-size: 15; -fx-padding: 15 20; -fx-cursor: hand;");
            }
        }

        if (activePage == null || activePage.isEmpty()) return;

        // 2. So sánh trực tiếp bằng tên file FXML thực tế đang chạy
        switch (activePage) {
            case "AdminDashboardView.fxml" -> applyActiveStyle(btnDashboard);
            case "ItemApprovalView.fxml" -> applyActiveStyle(btnApprove);
            case "ManageUserView.fxml" -> applyActiveStyle(btnUsers);
            case "ManageAuctionView.fxml" -> applyActiveStyle(btnAuctions);
            // Thêm file cảnh báo của bạn nếu có vào đây:
            // case "AdminAlertView.fxml" -> applyActiveStyle(btnAlerts);
        }
    }

    private void navigate(ActionEvent event, String fxmlPath, String title) {
        // Chặn bấm trùng trang
        if (fxmlPath.equals(activePage)) return;

        // Ghi đè trạng thái trước khi đổi màn hình
        activePage = fxmlPath;

        switchScene(event, "/views/" + fxmlPath, title);
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        navigate(event, "AdminDashboardView.fxml", "Tổng quan hệ thống");
    }

    @FXML
    private void handleApproveItem(ActionEvent event) {
        navigate(event, "ItemApprovalView.fxml", "Duyệt tài sản");
    }

    @FXML
    private void handleManageUsers(ActionEvent event) {
        navigate(event, "ManageUserView.fxml", "Quản lý người dùng");
    }

    @FXML
    private void handleManageAuctions(ActionEvent event) {
        navigate(event, "ManageAuctionView.fxml", "Quản lý phiên đấu giá");
    }

    private void applyActiveStyle(Button btn) {
        if (btn != null) {
            btn.setStyle("-fx-font-weight: bold; -fx-background-color: #e0e7ff; " +
                    "-fx-text-fill: #4f46e5; -fx-font-size: 15; -fx-padding: 15 20; " +
                    "-fx-cursor: hand; -fx-background-radius: 10;");
        }
    }
}