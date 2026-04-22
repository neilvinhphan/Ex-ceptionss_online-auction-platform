package org.example.client.controllers;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

public class MainController extends BaseController {
  @FXML private MenuButton menuDanhMuc;
  @FXML private MenuButton menuPhongDauGia;
  @FXML private MenuButton menuSearch;
@FXML private MenuButton menuUser;
  @FXML
  private void handleMenuItem(ActionEvent event) {
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
  void handleUserui(ActionEvent event) {try {
    Stage stage = (Stage) menuUser.getScene().getWindow();
    Parent root = FXMLLoader.load(getClass().getResource("/views/PersonalView.fxml"));
    stage.setScene(new Scene(root));
    stage.setTitle("Thông tin cá nhân");
  } catch (IOException e) {
    e.printStackTrace();
  }
  }
  @FXML
  void handleLogout(ActionEvent event){try {
    Stage stage = (Stage) menuUser.getScene().getWindow();
    Parent root = FXMLLoader.load(getClass().getResource("/views/LoginView.fxml"));
    stage.setScene(new Scene(root));
    stage.setTitle("Đăng nhập hệ thống");
  } catch (IOException e) {
    e.printStackTrace();
  }
  }
}

