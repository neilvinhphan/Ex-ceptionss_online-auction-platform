package org.example.client.controllers;

import javafx.event.ActionEvent;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

public class CreateAuctionController extends BaseController {
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
        switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");

    }
    public void handleCreateAuction(ActionEvent event) {
        switchScene(event, "/views/CreateAuctionView.fxml", "Tạo cuộc đấu giá");

    }
}
