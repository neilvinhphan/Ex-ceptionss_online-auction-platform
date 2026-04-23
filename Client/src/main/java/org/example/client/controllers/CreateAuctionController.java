package org.example.client.controllers;

import org.example.client.utils.UserSession;
import org.example.core.models.users.User;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

public class CreateAuctionController extends BaseController implements Initializable {
    @FXML
    private MenuButton menuUser;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            menuUser.setText(currentUser.getUserName());
        }
    }
    public void handleMain(ActionEvent event) {
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    public void handleMenuItem(ActionEvent event) {
        MenuItem item = (MenuItem) event.getSource();
        MenuButton parentMenu = (MenuButton) item.getParentPopup().getOwnerNode();
        parentMenu.setText(item.getText());
        switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục sản phẩm đấu giá");
    }
    public void handleRoomAuction(ActionEvent event) {
        MenuItem item = (MenuItem) event.getSource();
        MenuButton parentMenu = (MenuButton) item.getParentPopup().getOwnerNode();
        parentMenu.setText(item.getText());
    }

    public void handleUserui(ActionEvent event) {
        switchScene(event, "/views/PersonalView.fxml", "Hồ sơ cá nhân");

    }

    public void handleLogout(ActionEvent event) {
        UserSession.getInstance().cleanUserSession();
        switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");

    }
    public void handleCreateAuction(ActionEvent event) {
        switchScene(event, "/views/CreateAuctionView.fxml", "Tạo cuộc đấu giá");

    }
}
