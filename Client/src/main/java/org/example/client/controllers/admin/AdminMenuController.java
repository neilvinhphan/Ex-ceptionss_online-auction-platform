package org.example.client.controllers.admin;

import com.google.gson.Gson;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import java.util.logging.Logger;
import org.example.client.controllers.BaseController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.shared.enums.ActionType;

/** Controller quản lý thanh menu điều hướng của hệ thống Admin Panel. */
public class AdminMenuController extends BaseController {

  private static final Logger logger = Logger.getLogger(AdminMenuController.class.getName());

  /**
   * Xử lý sự kiện chuyển hướng từ giao diện quản trị sang giao diện đấu giá của người dùng.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  private void handleSwitchToUserUI(ActionEvent event) {
    logger.info("Yêu cầu chuyển hướng sang giao diện người dùng.");
    switchScene(event, "/views/AuctionCatalogView.fxml", "Trang chủ - AuctionPro");
  }

  /**
   * Xử lý hành động đăng xuất của tài khoản quản trị viên. Hiển thị hộp thoại xác nhận và tiến hành
   * xóa phiên làm việc nếu được đồng ý.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  private void handleLogout(ActionEvent event) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Xác nhận đăng xuất");
    alert.setHeaderText(null);
    alert.setContentText("Bạn có chắc chắn muốn đăng xuất khỏi Admin Panel?");

    Optional<ButtonType> result = alert.showAndWait();

    if (result.isPresent() && result.get() == ButtonType.OK) {
      logger.info("Admin xác nhận đăng xuất. Đang gửi yêu cầu giải phóng luồng lên Server...");

      try {
        Gson gson = ClientManager.getInstance().getGson();
        AuctionClient clientSocket = ClientManager.getInstance().getClient();

        Request logoutRequest = new Request(ActionType.LOGOUT, null);
        String jsonRequest = gson.toJson(logoutRequest);

        String jsonResponse = clientSocket.sendRequest(jsonRequest);
        if (jsonResponse != null) {
          Response response = gson.fromJson(jsonResponse, Response.class);
          if (!"SUCCESS".equals(response.getStatus())) {
            logger.warning("Server phản hồi lỗi xử lý đăng xuất [" + response.getData() + "]: " + response.getMessage());
          }
        }
      } catch (Exception e) {
        logger.severe("Lỗi kết nối mạng khi thực hiện gạch tên LOGOUT của Admin: " + e.getMessage());
      }

      UserSession.getInstance().cleanUserSession();
      switchScene(event, "/views/LoginView.fxml", "Đăng nhập");
    }
  }
}
