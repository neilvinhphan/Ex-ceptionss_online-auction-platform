package org.example.client.controllers;

import com.google.gson.Gson;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.core.dto.RegisterRequestDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.users.User;


import javafx.application.Platform;
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

  private final Gson gson = new Gson();
  private AuctionClient clientSocket = ClientManager.getInstance().getClient();
  @FXML
  void handleRegister(ActionEvent event) {
    String userName = tfuserName.getText();
    String phone = tfphone.getText();
    String email = tfemail.getText();
    String password = pass_an.isVisible() ? pass_an.getText() : pass_hien.getText();
    String repassword = repass_an.isVisible() ? repass_an.getText() : repass_hien.getText();
   // boolean checkCommit = cbCommit.isSelected();
    if (!password.equals(repassword)) {
      showAlert("Error", "Passwords do not matched ");
    } else if (!cbCommit.isSelected()) {
      showAlert("Thông báo", "Please accept the terms and conditions to proceed!");
    }
    try {
      RegisterRequestDTO registerRequestDTO = new RegisterRequestDTO(userName, phone, email, password);
      Request request = new Request("REGISTER", registerRequestDTO);
      String jsonRequest = gson.toJson(request);
      new Thread(() -> {
        try {
          String jsonResponse = clientSocket.sendRequest(jsonRequest);
          Response response = gson.fromJson(jsonResponse, Response.class);
          Platform.runLater(() -> {
            if (response.getStatus().equals("SUCCESS")) {
              showAlert("Thành công", "Đăng ký thành công! Chuyển sang trang đăng nhập...");
              switchScene(event, "/views/LoginView.fxml", "Đăng nhập");
            } else {
              showAlert("Đăng ký thất bại!", response.getMessage());
            }
          });
        } catch (Exception ex) {
          ex.printStackTrace();
          Platform.runLater(() -> showAlert("Lỗi kết nối", "Không thể kết nối đến server: " + ex.getMessage()));
        }
      }).start();
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
