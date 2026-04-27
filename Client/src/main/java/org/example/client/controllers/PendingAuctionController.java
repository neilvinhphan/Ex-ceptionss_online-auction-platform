package org.example.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class PendingAuctionController extends BaseController {
    @FXML
    public void handleMain(ActionEvent event) {
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    public void handleMenuItem(ActionEvent event) {
        switchScene(event, "/views/AuctionCatalogView.fxml", "Danh sách phòng đấu giá");
    }

    public void handleUserui(ActionEvent event) {
        switchScene(event, "/views/PersonalView.fxml", "Hồ sơ cá nhân");
    }

    public void handleLogout(ActionEvent event) {
        switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống ");
    }

    public void handleAddProduct(ActionEvent event) {
        switchScene(event, "/views/CreateAuctionView.fxml", "Thêm sản phẩm đấu giá");
    }

    public void handleEditProduct(ActionEvent event) {
        switchScene(event, "/views/CreateAuctionView.fxml", "Chỉnh sửa sản phẩm");
        // xóa cái cũ -> cập nhật
    }

    public void handleDeleteProduct(ActionEvent event) {

    }
}
