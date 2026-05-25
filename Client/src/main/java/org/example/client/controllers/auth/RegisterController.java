package org.example.client.controllers.auth;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.example.client.controllers.BaseController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.userDTO.RegisterRequestDTO;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller xử lý màn hình đăng ký tài khoản thành viên mới (Register). Kiểm tra tính hợp lệ dữ
 * liệu đầu vào và gửi lệnh tạo tài khoản lên Server qua cổng Socket.
 */
public class RegisterController extends BaseController {

  private static final Logger logger = Logger.getLogger(RegisterController.class.getName());

  @FXML private TextField tfUserName;
  @FXML private TextField tfPhone;
  @FXML private TextField tfEmail;
  @FXML private TextField passShow;
  @FXML private TextField repassShow;
  @FXML private PasswordField passHidden;
  @FXML private PasswordField repassHidden;
  @FXML private CheckBox cbCommit;

  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  /** Gắn phím tắt Enter vào toàn bộ các ô nhập dữ liệu, giúp gửi form đăng ký nhanh chóng. */
  @FXML
  public void initialize() {
    tfUserName.setOnAction(this::handleRegister);
    tfPhone.setOnAction(this::handleRegister);
    tfEmail.setOnAction(this::handleRegister);
    passHidden.setOnAction(this::handleRegister);
    passShow.setOnAction(this::handleRegister);
    repassHidden.setOnAction(this::handleRegister);
    repassShow.setOnAction(this::handleRegister);
  }

  /** Xử lý xác thực dữ liệu tại client và truyền thông tin đăng ký tài khoản mới lên máy chủ. */
  @FXML
  void handleRegister(ActionEvent event) {
    String userName = tfUserName.getText().trim();
    String phone = tfPhone.getText().trim();
    String email = tfEmail.getText().trim();
    String password = passHidden.isVisible() ? passHidden.getText() : passShow.getText();
    String repassword = repassHidden.isVisible() ? repassHidden.getText() : repassShow.getText();

    if (userName.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
      showAlert("Error", "Vui lòng nhập đầy đủ các trường thông tin bắt buộc!");
      return;
    }

    if (!password.equals(repassword)) {
      showAlert("Error", "Mật khẩu xác nhận không trùng khớp!");
      return;
    }

    try {
      RegisterRequestDTO registerRequestDTO =
          new RegisterRequestDTO(userName, phone, email, password);
      Request request = new Request("REGISTER", registerRequestDTO);
      String jsonRequest = gson.toJson(request);

      new Thread(
              () -> {
                try {
                  logger.info("Đang truyền gói tin REGISTER lên máy chủ hệ thống...");
                  String jsonResponse = clientSocket.sendRequest(jsonRequest);
                  Response response = gson.fromJson(jsonResponse, Response.class);

                  Platform.runLater(
                      () -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                          logger.log(Level.INFO, "Đăng ký thành công tài khoản mới: {0}", userName);
                          showAlert(
                              "Thành công", "Đăng ký thành công! Chuyển sang trang đăng nhập...");
                          switchScene(event, "/views/LoginView.fxml", "Đăng nhập");
                        } else {
                          logger.log(
                              Level.WARNING,
                              "Máy chủ báo lỗi tạo tài khoản: {0}",
                              response.getMessage());
                          showAlert("Đăng ký thất bại!", response.getMessage());
                        }
                      });
                } catch (Exception ex) {
                  Platform.runLater(
                      () ->
                          showAlert(
                              "Lỗi kết nối", "Không thể kết nối đến server: " + ex.getMessage()));
                  logger.log(
                      Level.SEVERE, "Gặp ngoại lệ luồng mạng khi xử lý đăng ký tài khoản", ex);
                }
              })
          .start();
    } catch (Exception e) {
      showAlert("Register Failed!", e.getMessage());
      logger.log(Level.SEVERE, "Lỗi không xác định khi đăng ký", e);
    }
  }

  /** Hiện/ẩn văn bản cho trường nhập Mật khẩu chính. */
  @FXML
  void DisplayPassword(ActionEvent event) {
    PasswordDisplayLogic(passHidden, passShow);
  }

  /** Hiện/ẩn văn bản cho trường nhập Xác nhận lại mật khẩu. */
  @FXML
  void ReDisplayPassword(ActionEvent event) {
    PasswordDisplayLogic(repassHidden, repassShow);
  }

  /** Quay lại màn hình đăng nhập hệ thống. */
  @FXML
  void handleLogin(ActionEvent event) {
    logger.info("Người dùng hủy đăng ký, quay lại giao diện Đăng nhập.");
    switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
  }
}
