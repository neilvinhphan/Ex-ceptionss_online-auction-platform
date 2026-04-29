package org.example.client.controllers;

import com.google.gson.Gson;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.LoginRequestDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.users.User;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController extends BaseController {

  @FXML private TextField tfUserName;
  @FXML private PasswordField pfPassHidden;
  @FXML private TextField tfPassShow;

  private Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  @FXML
  void handleLogin(ActionEvent event) throws Exception {
    String userName = tfUserName.getText();
    String password = pfPassHidden.getText();
    String passwordHidden = tfPassShow.getText();

    if (userName.isEmpty() || password.isEmpty()) {
      showAlert("Lỗi", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
      return;
    }
    System.out.println("Tạo thread");
    try {
      LoginRequestDTO loginRequestDTO = new LoginRequestDTO(userName, password);
      Request request = new Request("LOGIN", loginRequestDTO);
      String jsonRequest = gson.toJson(request);
      new Thread(
              () -> {
                try {
                  System.out.println("Gửi socket");
                  String jsonResponse = clientSocket.sendRequest(jsonRequest);
                  Response response = gson.fromJson(jsonResponse, Response.class);
                  System.out.println("Nhận phản hồi");
                  Platform.runLater(
                      () -> {
                        System.out.println(response.getStatus());
                        if (response.getStatus().equals("SUCCESS")) {
                          String dataUserJson = gson.toJson(response.getData());
                          User loggedInUser = gson.fromJson(dataUserJson, User.class);
                          UserSession.getInstance().setCurrentUser(loggedInUser);
                          System.out.println("Đăng nhập thành công! Người dùng: ");
                          showAlert("Thành công", "Đăng nhập thành công! Chuyển sang trang chủ...");
                          switchScene(event, "/views/MainView.fxml", "Trang chủ");
                        } else {
                          System.out.println("Đăng nhập thất bại: " + response.getMessage());
                          System.out.println("Message: " + response.getMessage());
                          System.out.println("Data: " + response.getData());
                          showAlert("Đăng nhập thất bại!", response.getMessage());
                        }
                      });
                } catch (Exception e) {
                  Platform.runLater(
                      () ->
                          showAlert(
                              "Lỗi kết nối", "Không thể kết nối đến server: " + e.getMessage()));
                }
              })
          .start();
    } catch (Exception e) {
      showAlert("Lỗi", "Đã xảy ra lỗi khi gửi yêu cầu đăng nhập: " + e.getMessage());
    }
  }

  @FXML
  void DisplayPassword(ActionEvent event) {
    PasswordDisplayLogic(pfPassHidden, tfPassShow);
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
