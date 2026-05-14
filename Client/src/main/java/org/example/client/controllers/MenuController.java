package org.example.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import org.example.client.utils.UserSession;
import org.example.core.models.users.User;
import org.example.core.shared.enums.RoleType;
import java.net.URL;
import java.util.ResourceBundle;

public class MenuController extends BaseController implements Initializable {

    @FXML private MenuButton menuUser;
    @FXML private Label roleLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Chỉ xử lý logic hiển thị tên và Role ở đây, code 1 lần dùng mãi mãi
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            menuUser.setText(currentUser.getUserName());
            // ... set Text cho roleLabel giống y như bạn đã làm ...
        }
    }

    @FXML void handleMain(ActionEvent event) {
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    @FXML void handleMenuItem(ActionEvent event) {
        switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục sản phẩm");
    }

    @FXML void handleUserUi(ActionEvent event) {
        switchScene(event, "/views/PersonalView.fxml", "Hồ sơ cá nhân");
    }

    @FXML void handleLogout(ActionEvent event) {
        UserSession.getInstance().cleanUserSession();
        switchScene(event, "/views/LoginView.fxml", "Đăng nhập");
    }
}