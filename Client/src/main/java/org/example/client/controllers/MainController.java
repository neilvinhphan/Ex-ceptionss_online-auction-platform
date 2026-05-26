package org.example.client.controllers;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.userDTO.UpdateRoleRequestDTO;
import org.example.core.models.users.User;
import org.example.core.shared.enums.RoleType;
import org.mindrot.jbcrypt.BCrypt;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller điều phối màn hình Trang chủ tổng quan hệ thống (Main View / Dashboard Client). Quản
 * lý banner quảng cáo nâng cấp tài khoản, phân quyền giao diện trang sảnh và nạp danh sách các
 * phòng đấu giá nổi bật (Promoted Auctions) từ hệ thống.
 */
public class MainController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(MainController.class.getName());

  @FXML private HBox upgradeBanner;
  @FXML private FlowPane promotedAuctionsContainer;

  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  /**
   * Khởi tạo giao diện trang chủ, ẩn giấu banner nâng cấp nếu tài khoản đã là Admin/Seller và kích
   * hoạt nạp danh sách phòng nổi bật.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    User currentUser = UserSession.getInstance().getCurrentUser();

    if (currentUser != null) {
      if (currentUser.getRole() == RoleType.ADMIN || currentUser.getRole() == RoleType.SELLER) {
        upgradeBanner.setVisible(false);
        upgradeBanner.setManaged(false);
      } else {
        upgradeBanner.setVisible(true);
        upgradeBanner.setManaged(true);
      }
    }
    loadPromotedAuctionsFromServer();
  }

  /**
   * Xử lý hiển thị hộp thoại Custom Dialog yêu cầu xác nhận mật khẩu hiện tại để tiến hành nâng cấp
   * quyền hạn lên nhóm tài khoản Người bán (Seller).
   */
  @FXML
  private void handleUpgrade(ActionEvent event) {
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle("Xác nhận nâng cấp");
    dialog.setHeaderText(
        "Bạn có chắc chắn muốn nâng cấp lên tài khoản SELLER không?\nNếu có, vui lòng nhập lại mật khẩu để xác nhận:");

    ButtonType confirmButtonType = new ButtonType("Nâng cấp", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

    PasswordField passwordField = new PasswordField();
    passwordField.setPromptText("Nhập mật khẩu hiện tại của bạn...");

    VBox vbox = new VBox();
    vbox.setSpacing(10);
    vbox.getChildren().add(passwordField);
    dialog.getDialogPane().setContent(vbox);

    Platform.runLater(passwordField::requestFocus);

    dialog.setResultConverter(
        dialogButton -> dialogButton == confirmButtonType ? passwordField.getText() : null);

    Optional<String> result = dialog.showAndWait();
    result.ifPresent(
        password -> {
          try {
            User currentUser = UserSession.getInstance().getCurrentUser();
            int currentId = currentUser.getUserId();

            logger.info("Khởi động tiến trình kiểm tra mật khẩu BCrypt nâng cấp quyền.");

            if (currentUser.getPassword() == null || currentUser.getPassword().isEmpty()) {
              showAlert(
                  "Lỗi hệ thống",
                  "Dữ liệu User trong Session không chứa mật khẩu mã hóa. Vui lòng kiểm tra lại API Login phía Server!");
              return;
            }

            if (!BCrypt.checkpw(password, currentUser.getPassword())) {
              showAlert("Lỗi", "Sai mật khẩu!");
              return;
            }

            logger.info(
                "Xác thực BCrypt thành công nội bộ. Chuẩn bị truyền tín hiệu lên Server...");
            sendUpgradeRequestToServer(currentId, event);

          } catch (Exception e) {
            logger.log(
                Level.SEVERE, "Lỗi bất ngờ phát sinh trong quá trình so khớp mã băm BCrypt", e);
            showAlert("Lỗi vặt", "Có lỗi xảy ra khi kiểm tra mật khẩu: " + e.getMessage());
          }
        });
  }

  /**
   * Tạo đa luồng ngầm gửi yêu cầu Socket mang mã hành động "UPDATE_ROLE" lên máy chủ xử lý cơ sở dữ
   * liệu.
   */
  private void sendUpgradeRequestToServer(int userId, ActionEvent event) {
    UpdateRoleRequestDTO updateRoleRequestDTO = new UpdateRoleRequestDTO(userId);
    Request request = new Request("UPDATE_ROLE", updateRoleRequestDTO);
    String jsonRequest = gson.toJson(request);

    new Thread(() -> {
      try {
        String jsonResponse = clientSocket.sendRequest(jsonRequest);
        Response response = gson.fromJson(jsonResponse, Response.class);

        Platform.runLater(() -> {
          if ("SUCCESS".equals(response.getStatus())) {
            showAlert("Chúc mừng", "Nâng cấp thành công! Vui lòng đăng nhập lại.");
            UserSession.getInstance().cleanUserSession();
            switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
          } else {
            showAlert("Đã xảy ra lỗi", "Mật khẩu không chính xác! Vui lòng nhập lại.");
          }
        });
      } catch (Exception e) {
        Platform.runLater(() -> showAlert("Lỗi kết nối", "Không thể gửi yêu cầu: " + e.getMessage()));
        logger.log(Level.SEVERE, "Gặp sự cố ngắt kết nối Socket", e);
      }
    }).start();
  }

  /**
   * Nhận danh sách phân vùng các phòng đấu giá được đẩy lên trang đầu (Promoted) từ phía máy chủ.
   */
  private void loadPromotedAuctionsFromServer() {
    // TODO: Thực hiện kết nối Socket nhận dữ liệu vẽ thẻ Card nổi bật thông qua hành động
    // "GET_PROMOTED_AUCTIONS"
  }
}
