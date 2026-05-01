package org.example.client.controllers;

import com.google.gson.Gson;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.core.dto.RegisterRequestDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController extends BaseController {
  @FXML private TextField tfUserName;
  @FXML private TextField tfPhone;
  @FXML private TextField tfEmail;
  @FXML private TextField passShow;
  @FXML private TextField repassShow;
  @FXML private PasswordField passHidden;
  @FXML private PasswordField repassHidden;
  @FXML private CheckBox cbCommit;

  private Gson gson = ClientManager.getInstance().getGson();
  private AuctionClient clientSocket = ClientManager.getInstance().getClient();
  @FXML
  void handleRegister(ActionEvent event) {
    String userName = tfUserName.getText();
    String phone = tfPhone.getText();
    String email = tfEmail.getText();
    String password = passHidden.isVisible() ? passHidden.getText() : passShow.getText();
    String repassword = repassHidden.isVisible() ? repassHidden.getText() : repassShow.getText();
   // boolean checkCommit = cbCommit.isSelected();
    if (!password.equals(repassword)) {
      showAlert("Error", "Passwords do not matched ");
    }
//      else if (!cbCommit.isSelected()) {
//      showAlert("Thông báo", "Please accept the terms and conditions to proceed!");
//    }
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
              System.out.println(response.getStatus());
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
    PasswordDisplayLogic(passHidden, passShow);
  }

  @FXML
  void ReDisplayPassword(ActionEvent event) {
    PasswordDisplayLogic(repassHidden, repassShow);
  }

  @FXML
  void handleLogin(ActionEvent event) {
    switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
    System.out.println("Sang trang login");
  }
//
//  @FXML
//  void handleDieuKhoan(ActionEvent event) {
//    System.out.println("Sang trang Dieu Khoan");
//  }
}
