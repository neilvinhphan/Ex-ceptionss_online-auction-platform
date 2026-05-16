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

    // Lưu trữ trang hiện tại để tô màu và chặn click trùng
    private static String currentView = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Platform.runLater giúp đợi giao diện gắn vào cửa sổ (Stage) xong rồi mới chạy
        javafx.application.Platform.runLater(() -> {
            if (btnProfile.getScene() != null && btnProfile.getScene().getWindow() != null) {
                javafx.stage.Stage stage = (javafx.stage.Stage) btnProfile.getScene().getWindow();
                String title = stage.getTitle();

                // Dựa vào tiêu đề trang (được truyền từ hàm switchScene) để set lại currentView
                if (title != null) {
                    if (title.contains("Hồ sơ")) currentView = "PersonalView.fxml";
                    else if (title.contains("Thanh toán")) currentView = "WaitPaymentView.fxml";
                    else if (title.contains("Kho hàng")) currentView = "WareHouseView.fxml";
                    else if (title.contains("Lịch sử")) currentView = "AuctionHistoryView.fxml";
                    else if (title.contains("Tạo sản phẩm")) currentView = "CreateItemView.fxml";
                    else if (title.contains("Tạo đấu giá")) currentView = "CreateAuctionView.fxml";
                    else if (title.contains("Doanh thu")) currentView = "RevenueView.fxml";
                    else currentView = ""; // Nếu đang ở Catalog, không highlight nút nào
                }
            }

            // Chạy hàm tô màu sau khi đã "chỉnh đốn" lại biến currentView
            applyActiveStyle();
        });
    }

    /**
     * Tự động tô màu nút dựa trên biến currentView
     * Sử dụng Style Class để giao diện chuyên nghiệp hơn
     */
    private void applyActiveStyle() {
        // Xóa sạch class active cũ của tất cả các nút
        for (Node node : new Node[]{btnProfile, btnWaitPayment, btnWarehouse, btnHistory, btnCreateItem,btnCreateAuction,btnRevenue}) {
            node.getStyleClass().remove("sidebar-active");
        }

        // Áp dụng class active cho đúng nút
        switch (currentView) {
            case "PersonalView.fxml" -> btnProfile.getStyleClass().add("sidebar-active");
            case "WaitPaymentView.fxml" -> btnWaitPayment.getStyleClass().add("sidebar-active");
            case "WareHouseView.fxml" -> btnWarehouse.getStyleClass().add("sidebar-active");
            case "AuctionHistoryView.fxml" -> btnHistory.getStyleClass().add("sidebar-active");
            case "CreateItemView.fxml" -> btnCreateItem.getStyleClass().add("sidebar-active");
            case "RevenueView.fxml" -> btnRevenue.getStyleClass().add("sidebar-active");

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