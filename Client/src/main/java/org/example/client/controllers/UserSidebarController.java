package org.example.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import org.example.client.utils.UserSession;
import org.example.core.models.users.User;
import org.example.core.shared.enums.RoleType;

import java.net.URL;
import java.util.ResourceBundle;

public class UserSidebarController extends BaseController implements Initializable {
    @FXML private Button btnProfile, btnWaitPayment, btnWarehouse, btnHistory, btnCreateItem, btnCreateAuction;

    // Lưu trữ trang hiện tại để tô màu và chặn click trùng
    private static String currentView = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyActiveStyle();
    }

    /**
     * Tự động tô màu nút dựa trên biến currentView
     * Sử dụng Style Class để giao diện chuyên nghiệp hơn
     */
    private void applyActiveStyle() {
        // Xóa sạch class active cũ của tất cả các nút
        for (Node node : new Node[]{btnProfile, btnWaitPayment, btnWarehouse, btnHistory, btnCreateItem}) {
            node.getStyleClass().remove("sidebar-active");
        }

        // Áp dụng class active cho đúng nút
        switch (currentView) {
            case "PersonalView.fxml" -> btnProfile.getStyleClass().add("sidebar-active");
            case "WaitPaymentView.fxml" -> btnWaitPayment.getStyleClass().add("sidebar-active");
            case "WareHouseView.fxml" -> btnWarehouse.getStyleClass().add("sidebar-active");
            case "AuctionHistoryView.fxml" -> btnHistory.getStyleClass().add("sidebar-active");
            case "CreateItemView.fxml" -> btnCreateItem.getStyleClass().add("sidebar-active");
        }
    }

    /**
     * Hàm điều hướng thông minh:
     * 1. Chặn click vào trang đang đứng.
     * 2. Tự động lưu view hiện tại.
     */
    private void navigate(ActionEvent event, String fxmlPath, String title, boolean requireSeller) {
        if (currentView.equals(fxmlPath)) return; // Chặn click trùng

        if (requireSeller) {
            User user = UserSession.getInstance().getCurrentUser();
            if (user == null || (user.getRole() != RoleType.SELLER && user.getRole() != RoleType.ADMIN)) {
                showAlert("Quyền truy cập", "Tính năng này chỉ dành cho SELLER. Vui lòng nâng cấp tài khoản!");
                return;
            }
        }

        currentView = fxmlPath;
        switchScene(event, "/views/" + fxmlPath, title);
    }

    @FXML private void handleUserUi(ActionEvent event) {
        navigate(event, "PersonalView.fxml", "Hồ sơ cá nhân", false);
    }

    @FXML private void handleWaitPayment(ActionEvent event) {
        navigate(event, "WaitPaymentView.fxml", "Thanh toán", false);
    }

    @FXML private void handleWareHouse(ActionEvent event) {
        navigate(event, "WareHouseView.fxml", "Kho hàng", true);
    }

    @FXML private void handleHistoryAuction(ActionEvent event) {
        navigate(event, "AuctionHistoryView.fxml", "Lịch sử", false);
    }

    @FXML private void handleCreateItem(ActionEvent event) {
        navigate(event, "CreateItemView.fxml", "Tạo sản phẩm", true);
    }

    @FXML private void handleCreateAuction(ActionEvent event) {
        navigate(event, "CreateAuctionView.fxml", "Tạo đấu giá", true);
    }
}