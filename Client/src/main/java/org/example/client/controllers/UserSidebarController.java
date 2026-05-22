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
    @FXML private Button btnProfile, btnWaitPayment, btnWarehouse, btnHistory, btnCreateItem, btnCreateAuction,btnRevenue;

    public static String currentView = "PersonalView.fxml";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Đảm bảo lúc nào load giao diện lên cũng bôi màu đúng theo view đang mở
        applyActiveStyle();
    }
    /**
     * Tự động tô màu nút dựa trên biến currentView
     * Sử dụng Style Class để giao diện chuyên nghiệp hơn
     */
    private void applyActiveStyle() {
        // Xóa sạch class active cũ của tất cả các nút
        for (Node node : new Node[]{btnProfile, btnWaitPayment, btnWarehouse, btnHistory, btnCreateItem, btnCreateAuction, btnRevenue}) {
            if (node != null) {
                node.getStyleClass().remove("sidebar-active");
            }
        }

        if (currentView == null || currentView.isEmpty()) return;

        switch (currentView) {
            case "PersonalView.fxml" -> { if(btnProfile != null) btnProfile.getStyleClass().add("sidebar-active"); }
            case "WaitPaymentView.fxml" -> { if(btnWaitPayment != null) btnWaitPayment.getStyleClass().add("sidebar-active"); }
            case "WareHouseView.fxml" -> { if(btnWarehouse != null) btnWarehouse.getStyleClass().add("sidebar-active"); }
            case "AuctionHistoryView.fxml" -> { if(btnHistory != null) btnHistory.getStyleClass().add("sidebar-active"); }
            case "CreateItemView.fxml" -> { if(btnCreateItem != null) btnCreateItem.getStyleClass().add("sidebar-active"); }
            case "CreateAuctionView.fxml" -> { if(btnCreateAuction != null) btnCreateAuction.getStyleClass().add("sidebar-active"); }
            case "RevenueView.fxml" -> { if(btnRevenue != null) btnRevenue.getStyleClass().add("sidebar-active"); }
            default -> {
                // Nếu ở trang nằm ngoài sidebar (như Catalog công cộng), ta có thể mặc định sáng nút Hồ sơ
                // hoặc để trống tùy bạn. Nếu muốn mặc định sáng nút Hồ sơ khi mới vào:
                if(btnProfile != null) btnProfile.getStyleClass().add("sidebar-active");
            }
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
            if (user == null || (user.getRole() != RoleType.SELLER )) {
                showAlert("Quyền truy cập", "Tính năng này chỉ dành cho SELLER!");
                return;
            }
        }

        currentView = fxmlPath;
        applyActiveStyle();
        switchScene(event, "/views/" + fxmlPath, title);
    }

    @FXML private void handleUserUi(ActionEvent event) {
        navigate(event, "PersonalView.fxml", "Hồ sơ", false);
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
@FXML
    public void handleRevenue(ActionEvent event) {
    navigate(event, "RevenueView.fxml", "Doanh thu", true);

    }
}