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

public class AuctionCatalogController extends BaseController implements Initializable {
    @FXML
    private MenuButton menuUser;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            menuUser.setText(currentUser.getUserName());
        }
    }
    @FXML
    public void handleMain(ActionEvent event) {
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    @FXML
    public void handleMenuItem(ActionEvent event) {
        MenuItem item = (MenuItem) event.getSource();
        MenuButton parentMenu = (MenuButton) item.getParentPopup().getOwnerNode();
        parentMenu.setText(item.getText());
        switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục sản phẩm đấu giá");
    }

    @FXML
    public void handleUserui(ActionEvent event) {
        switchScene(event, "/views/PersonalView.fxml", "Hồ sơ cá nhân");

    }

    @FXML
    public void handleLogout(ActionEvent event) {

        UserSession.getInstance().cleanUserSession();
        switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
    }

    public void handleJoinAuction(ActionEvent event) {
        switchScene(event, "/views/AuctionRoomView.fxml", "Phòng đấu giá trực tiếp");
    }
}
