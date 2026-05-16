package org.example.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import org.example.client.utils.UserSession;
import org.example.core.models.users.User;
import org.example.core.shared.enums.RoleType;
import java.net.URL;
import java.util.ResourceBundle;

public class UserMenuController extends BaseController implements Initializable {

    @FXML private MenuButton menuUser;
    @FXML private Label roleLabel;
    @FXML private Button btnBackToAdmin;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = UserSession.getInstance().getCurrentUser();

        if (currentUser != null) {
            menuUser.setText(currentUser.getUserName());

            // Kiểm tra quyền Admin để hiện nút đặc biệt
            if (currentUser.getRole() == RoleType.ADMIN) {
                roleLabel.setText("Admin");
                roleLabel.setStyle("-fx-text-fill: #e63946; -fx-font-size: 11; -fx-font-weight: bold;");

                // HIỆN NÚT QUAY LẠI CHO ADMIN
                btnBackToAdmin.setVisible(true);
                btnBackToAdmin.setManaged(true);

            } else if (currentUser.getRole() == RoleType.SELLER) {
                roleLabel.setText("Seller");
                roleLabel.setStyle("-fx-text-fill: #ffc107; -fx-font-size: 11; -fx-font-weight: bold;");
            } else {
                roleLabel.setText("Bidder");
                roleLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11; -fx-font-style: italic;");
            }
        }
    }

    @FXML void handleMain(ActionEvent event) {
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    @FXML void handleMenuItem(ActionEvent event) {
        switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục sản phẩm");
    }

    @FXML void handleUserUi(ActionEvent event) {
        UserSidebarController.currentView = "PersonalView.fxml";
        switchScene(event, "/views/PersonalView.fxml", "Hồ sơ cá nhân");
    }

    @FXML void handleLogout(ActionEvent event) {
        UserSession.getInstance().cleanUserSession();
        switchScene(event, "/views/LoginView.fxml", "Đăng nhập");
    }

    @FXML
    void handleBackToAdmin(ActionEvent event) {
        AdminSidebarController.activePage = "AdminDashboardView.fxml";
        switchScene(event, "/views/AdminDashboardView.fxml", "Tổng quan hệ thống - Admin");
    }
}