package org.example.client.controllers;

import org.example.core.dto.LoginRequestDTO;
import org.example.core.dto.RegisterRequestDTO;
import org.example.core.models.users.User;
import org.example.server.services.AuthService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController extends BaseController {
  @FXML private TextField tfuserName;
  @FXML private TextField tfphone;
  @FXML private TextField tfemail;
  @FXML private TextField pass_hien;
  @FXML private TextField repass_hien;
  @FXML private PasswordField pass_an;
  @FXML private PasswordField repass_an;
  @FXML private CheckBox cbCommit;

  @FXML
  void handleRegister(ActionEvent event) {
    String userName = tfuserName.getText();
    String phone = tfphone.getText();
    String email = tfemail.getText();
    String password = pass_an.getText();
    String passwordhidden = pass_hien.getText();
    String repassword = repass_an.getText();
    String repasswordhidden = repass_hien.getText();
    boolean checkCommit = cbCommit.isSelected();
    try {
      RegisterRequestDTO registerRequestDTO =
          new RegisterRequestDTO(userName, phone, email, password);
      User checkRegister = AuthService.register(registerRequestDTO);
      switchScene(event, "/views/LoginView.fxml", "Đăng nhập");
    } catch (Exception e) {
      showAlert("Register Failed!", e.getMessage());
    }
  }

  @FXML
  void DisplayPassword(ActionEvent event) {
    PasswordDisplayLogic(pass_an, pass_hien);
  }

  @FXML
  void ReDisplayPassword(ActionEvent event) {
    PasswordDisplayLogic(repass_an, repass_hien);
  }

  @FXML
  void handleLogin(ActionEvent event) {
    System.out.println("Sang trang login");
    switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
  }

  @FXML
  void handleDieuKhoan(ActionEvent event) {
    System.out.println("Sang trang Dieu Khoan");
  }
}
