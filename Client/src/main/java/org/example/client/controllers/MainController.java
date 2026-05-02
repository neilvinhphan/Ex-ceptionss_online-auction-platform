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

public class MainController extends BaseController implements Initializable {
  @FXML private MenuButton menuDanhMuc;
  @FXML private MenuButton menuPhongDauGia;
  @FXML private MenuButton menuSearch;
  @FXML private MenuButton menuUser;
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    User currentUser = UserSession.getInstance().getCurrentUser();
    if (currentUser != null) {
      menuUser.setText(currentUser.getUserName());
    }
  }

  @FXML
  private void handleMenuItem(ActionEvent event) {
    MenuItem item = (MenuItem) event.getSource();
    MenuButton parentMenu = (MenuButton) item.getParentPopup().getOwnerNode();
    parentMenu.setText(item.getText());
    switchScene(event,"/views/AuctionCatalogView.fxml","Danh mục sản phẩm đấu giá");
  }

  @FXML
  private void handleRoomAuction(ActionEvent event){
  MenuItem item = (MenuItem) event.getSource();
  MenuButton parentMenu = (MenuButton) item.getParentPopup().getOwnerNode();
  parentMenu.setText(item.getText());
}

  @FXML
  void handleMain(ActionEvent event) {
    switchScene(event, "/views/MainView.fxml", "Trang chủ");
  }

  @FXML
  void handleLogin(ActionEvent event) {
    switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
  }

  @FXML
  void handleRegister(ActionEvent event) {
    switchScene(event, "/views/RegisterView.fxml", "Đăng ký tài khoản");
  }

  @FXML
  void handleUserUi(ActionEvent event) {
    switchScene(event, "/views/PersonalView.fxml", "Hồ sơ cá nhân");
  }

  @FXML
  void handleLogout(ActionEvent event){
    UserSession.getInstance().cleanUserSession();
    switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
  }

  @FXML
  void handleButtonItem(ActionEvent event){
    switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục sản phẩm");
  }
}

