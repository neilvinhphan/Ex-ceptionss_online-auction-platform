package org.example.client.controllers.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import java.util.logging.Logger;
import org.example.client.controllers.BaseController;
import org.example.client.utils.UserSession;

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
      logger.info("Admin xác nhận đăng xuất. Đang xóa phiên làm việc.");
      UserSession.getInstance().cleanUserSession();
      switchScene(event, "/views/LoginView.fxml", "Đăng nhập ");
    }
  }
}
