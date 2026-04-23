package org.example.client.controllers;

import org.example.client.utils.UserSession;
import org.example.core.dto.LoginRequestDTO;
import org.example.core.models.users.User;
import org.example.server.services.AuthService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController extends BaseController {

  @FXML private TextField tfuserName;
  @FXML private PasswordField pass_an;
  @FXML private TextField pass_hien;

  @FXML
  void handleLogin(ActionEvent event) throws Exception {
    String userName = tfuserName.getText();
    String password = pass_an.getText();
    String passwordhidden = pass_hien.getText();

    if (userName.isEmpty() || password.isEmpty()) {
      showAlert("Lỗi", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
      return;
    }

    try {
      LoginRequestDTO loginRequestDTO = new LoginRequestDTO(userName, password);
      User checkLogin = AuthService.login(loginRequestDTO);
      if (checkLogin != null) {
        System.out.println("Đăng nhập thành công! Chuyển sang trang chủ...");
        UserSession.getInstance().setCurrentUser(checkLogin);
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
      }
    } catch (Exception e) {
      showAlert("Login Failed!", e.getMessage());
    }
  }

  @FXML
  void DisplayPassword(ActionEvent event) {
    PasswordDisplayLogic(pass_an, pass_hien);
  }

  @FXML
  void handleRegister(ActionEvent event) {
    System.out.println("Đang chuyển sang trang Đăng ký...");
    switchScene(event, "/views/RegisterView.fxml", "Đăng ký tài khoản ");
  }

  @FXML
  void handleForgotPassword(ActionEvent event) {
    System.out.println("Mở form lấy lại mật khẩu...");
  }
}
