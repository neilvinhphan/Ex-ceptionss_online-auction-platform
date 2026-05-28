package org.example.client.controllers.auth;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.example.client.controllers.BaseController;
import org.example.client.controllers.admin.AdminSidebarController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.userDTO.LoginRequestDTO;
import org.example.core.models.users.User;
import org.example.core.shared.enums.ActionType;
import org.example.core.shared.enums.RoleType;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller xử lý logic đăng nhập hệ thống đấu giá trực tuyến. Quản lý kết nối Socket xác thực tài
 * khoản và phân luồng điều hướng người dùng/quản trị viên.
 */
public class LoginController extends BaseController {

  private static final Logger logger = Logger.getLogger(LoginController.class.getName());

  @FXML private TextField tfUserName;
  @FXML private PasswordField pfPass;
  @FXML private TextField tfPassShow;

  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  /** Khởi tạo và thiết lập phím tắt Enter cho các trường nhập liệu để kích hoạt đăng nhập nhanh. */
  @FXML
  public void initialize() {
    tfUserName.setOnAction(this::triggerLogin);
    pfPass.setOnAction(this::triggerLogin);
    tfPassShow.setOnAction(this::triggerLogin);
  }

  /** Xử lý sự kiện khi người dùng nhấn nút Đăng nhập hoặc ấn phím Enter. */
  @FXML
  void handleLogin(ActionEvent event) {
    String userName = tfUserName.getText().trim();
    String password = pfPass.isVisible() ? pfPass.getText() : tfPassShow.getText();

    if (userName.isEmpty() || password.isEmpty()) {
      showAlert("Lỗi", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
      return;
    }

    try {
      LoginRequestDTO loginRequestDTO = new LoginRequestDTO(userName, password);
      Request request = new Request(ActionType.LOGIN, loginRequestDTO);
      String jsonRequest = gson.toJson(request);

      new Thread(() -> {
        try {
          String jsonResponse = clientSocket.sendRequest(jsonRequest);
          Response response = gson.fromJson(jsonResponse, Response.class);

          Platform.runLater(() -> {
            if ("SUCCESS".equals(response.getStatus())) {
              String dataUserJson = gson.toJson(response.getData());
              User loggedInUser = gson.fromJson(dataUserJson, User.class);
              UserSession.getInstance().setCurrentUser(loggedInUser);

              if (loggedInUser.getRole() != null && loggedInUser.getRole() == RoleType.ADMIN) {
                showAlert("Thành công", "Đăng nhập thành công! Chuyển đến Admin Panel...");
                AdminSidebarController.activePage = "AdminDashboardView.fxml";
                switchScene(event, "/views/AdminDashboardView.fxml", "Tổng quan hệ thống - Admin");
              } else {
                showAlert("Thành công", "Đăng nhập thành công! Chuyển sang trang chủ...");
                switchScene(event, "/views/AuctionCatalogView.fxml", "Trang chủ");
              }
            } else {
              int code = response.getData() instanceof Number ? ((Number) response.getData()).intValue() : -1;
              String title = switch (code) {
                case 4010 -> "Sai thông tin (401)";
                case 4090 -> "Đang đăng nhập nơi khác (409)";
                case 4000 -> "Tài khoản không hợp lệ (400)";
                case 5000 -> "Lỗi hệ thống Server (500)";
                default -> "Đăng nhập thất bại (" + code + ")";
              };
              logger.log(Level.WARNING, "Đăng nhập thất bại từ hệ thống: {0}", response.getMessage());
              showAlert(title, response.getMessage());
            }
          });
        } catch (Exception e) {
          Platform.runLater(() -> showAlert("Lỗi kết nối", "Không thể kết nối đến server: " + e.getMessage()));
        }
      }).start();
    } catch (Exception e) {
      showAlert("Lỗi", "Đã xảy ra lỗi khi gửi yêu cầu đăng nhập: " + e.getMessage());
    }
  }

  /** Xử lý sự kiện hiển thị/ẩn mật khẩu văn bản thuần công khai trên UI. */
  @FXML
  void DisplayPassword(ActionEvent event) {
    PasswordDisplayLogic(pfPass, tfPassShow);
  }

  /** Điều hướng người dùng sang màn hình Đăng ký tài khoản mới. */
  @FXML
  void handleRegister(ActionEvent event) {
    logger.info("Điều hướng người dùng sang màn hình Đăng ký.");
    switchScene(event, "/views/RegisterView.fxml", "Đăng ký tài khoản");
  }

  /** Xử lý sự kiện khi người dùng yêu cầu lấy lại mật khẩu đã quên. */
  @FXML
  void handleForgotPassword(ActionEvent event) {
    logger.info("Người dùng yêu cầu mở form Khôi phục mật khẩu.");
  }

  /** Phương thức trợ năng kích hoạt xử lý đăng nhập có bọc kiểm tra ngoại lệ. */
  private void triggerLogin(ActionEvent event) {
    try {
      handleLogin(event);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Ngoại lệ nghiêm trọng xảy ra tại điểm kích hoạt đăng nhập", e);
    }
  }
}
