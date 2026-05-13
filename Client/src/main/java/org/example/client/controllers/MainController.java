package org.example.client.controllers;

import com.google.gson.Gson;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.UpdateRoleRequestDTO;
import org.example.core.models.users.User;
import org.example.core.shared.enums.RoleType;
import org.mindrot.jbcrypt.BCrypt;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuButton;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

public class MainController extends BaseController implements Initializable {
  @FXML private MenuButton menuDanhMuc;
  @FXML private MenuButton menuPhongDauGia;
  @FXML private MenuButton menuSearch;
  @FXML private MenuButton menuUser;
  private Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();
  @FXML VBox userBox, adminBox;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    User currentUser = UserSession.getInstance().getCurrentUser();
    //    if (currentUser != null) {
    //      menuUser.setText(currentUser.getUserName());
    if (currentUser != null && currentUser.getRole() == RoleType.ADMIN) {

      // Nếu là Admin -> Ẩn toàn bộ giao diện User
      userBox.setVisible(false);
      userBox.setManaged(false);

      // Bật giao diện Admin lên
      adminBox.setVisible(true);
      adminBox.setManaged(true);

    } else {
      // Nếu là người thường/Seller -> Ẩn giao diện Admin
      adminBox.setVisible(false);
      adminBox.setManaged(false);

      // Bật giao diện User lên
      userBox.setVisible(true);
      userBox.setManaged(true);
    }
  }

  @FXML
  private void handleMenuItem(ActionEvent event) {
    switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục sản phẩm đấu giá");
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
  void handleLogout(ActionEvent event) {
    UserSession.getInstance().cleanUserSession();
    switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
  }

  @FXML
  private void handleUpgrade(javafx.event.ActionEvent event) {
    // 1. Tạo hộp thoại Custom
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle("Xác nhận nâng cấp");
    dialog.setHeaderText(
        "Bạn có chắc chắn muốn nâng cấp lên tài khoản SELLER không?\nNếu có, vui lòng nhập lại mật khẩu để xác nhận:");

    // 2. Tạo các nút bấm (Xác nhận / Hủy)
    ButtonType confirmButtonType = new ButtonType("Nâng cấp", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

    // 3. Tạo trường nhập mật khẩu (PasswordField ẩn ký tự)
    PasswordField passwordField = new PasswordField();
    passwordField.setPromptText("Nhập mật khẩu hiện tại của bạn...");

    // 4. Đưa trường nhập vào Giao diện hộp thoại
    VBox vbox = new VBox();
    vbox.setSpacing(10);
    vbox.getChildren().add(passwordField);
    dialog.getDialogPane().setContent(vbox);

    // Xử lý focus mặc định vào trường mật khẩu khi mở popup
    javafx.application.Platform.runLater(passwordField::requestFocus);

    // 5. Bắt sự kiện khi bấm nút "Nâng cấp"
    dialog.setResultConverter(
        dialogButton -> {
          if (dialogButton == confirmButtonType) {
            return passwordField.getText();
          }
          return null;
        });

    // 6. Hiển thị hộp thoại và chờ người dùng nhập
    Optional<String> result = dialog.showAndWait();

    // 7. Xử lý kết quả sau khi nhập
    result.ifPresent(
        password -> {
          User currentUser = UserSession.getInstance().getCurrentUser();
          int currentId = currentUser.getUserId();
          if (!BCrypt.checkpw(password, currentUser.getPassword())) {
            showAlert("Lỗi", "Sai mật khẩu!");
            return;
          } else if (password.trim().isEmpty()) {
            showAlert("Cảnh báo", "Mật khẩu không được để trống!");
            return;
          }

          System.out.println("Gửi request nâng cấp với mật khẩu: " + password);
          sendUpgradeRequestToServer(currentId);
        });
  }

  private void sendUpgradeRequestToServer(int userId) {
    // 1. Lấy thông tin user đang login

    // 2. Gói vào DTO (ví dụ UpgradeRoleDTO gồm username và password)
    UpdateRoleRequestDTO updateRoleRequestDTO = new UpdateRoleRequestDTO(userId);
    // 3. Gửi Socket lên Server
    Request request = new Request("UPDATE_ROLE", updateRoleRequestDTO);
    String jsonRequest = gson.toJson(request);
    // 4. Nhận phản hồi:
    //    - Nếu Server báo "SUCCESS" -> Chúc mừng, báo đăng nhập lại để cập nhật menu.
    //    - Nếu Server báo "ERROR" (sai pass) -> In ra Alert lỗi.
    new Thread(
            () -> {
              try {
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);
                Platform.runLater(
                    () -> {
                      if ("SUCCESS".equals(response.getStatus())) {
                        showAlert("Chúc mừng", "Đăng nhập lại để cập nhật menu.");
                      } else {
                        showAlert("Đã xảy ra lỗi", "Mật khẩu không chính xác! Vui lòng nhập lại.");
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(
                    () -> showAlert("Lỗi kết nối", "Không thể gửi yêu cầu: " + e.getMessage()));
                e.printStackTrace();
              }
            })
        .start();
  }

  public void handleManageUsers(ActionEvent event) {
    switchScene(event, "/views/ManageUserView.fxml", "Quản lý người dùng");
  }

  public void handleManageAuctions(ActionEvent event) {
    switchScene(event, "/views/ManageAuctionView.fxml", "Quản lý phiên đấu giá");
  }

  public void handleApproveItem(ActionEvent event) {
    switchScene(event, "/views/ItemApprovalView.fxml", "Kiểm duyệt sản phẩm");
  }
}
