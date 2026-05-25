package org.example.client.controllers.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import org.example.client.controllers.BaseController;

/** Controller quản lý thanh điều hướng bên hông (Sidebar) của giao diện Admin Panel. */
public class AdminSidebarController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(AdminSidebarController.class.getName());

  public static String activePage = "AdminDashboardView.fxml";

  @FXML private Button btnDashboard;
  @FXML private Button btnApprove;
  @FXML private Button btnUsers;
  @FXML private Button btnAuctions;
  @FXML private Button btnAlerts;

  /** Khởi tạo và tự động đánh dấu nút chức năng đang hoạt động dựa trên trang hiện tại. */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    highlightActiveButton();
  }

  /**
   * Thực hiện thiết lập lại kiểu dáng (style) của tất cả các nút về mặc định, sau đó highlight nút
   * tương ứng với trang FXML đang hiển thị.
   */
  private void highlightActiveButton() {
    for (Node node : new Node[] {btnDashboard, btnApprove, btnUsers, btnAuctions, btnAlerts}) {
      if (node != null) {
        node.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #555555; -fx-font-weight: bold; -fx-font-size: 15; -fx-padding: 15 20; -fx-cursor: hand;");
      }
    }

    if (activePage == null || activePage.isEmpty()) return;

    switch (activePage) {
      case "AdminDashboardView.fxml" -> applyActiveStyle(btnDashboard);
      case "ItemApprovalView.fxml" -> applyActiveStyle(btnApprove);
      case "ManageUserView.fxml" -> applyActiveStyle(btnUsers);
      case "ManageAuctionView.fxml" -> applyActiveStyle(btnAuctions);
    }
  }

  /**
   * Điều hướng sang màn hình FXML được chỉ định và cập nhật lại trạng thái trang hoạt động.
   *
   * @param event Sự kiện kích hoạt từ UI.
   * @param fxmlPath Tên file FXML đích.
   * @param title Tiêu đề của cửa sổ hiển thị mới.
   */
  private void navigate(ActionEvent event, String fxmlPath, String title) {
    if (fxmlPath.equals(activePage)) return;

    logger.info("Yêu cầu chuyển màn hình từ [" + activePage + "] sang [" + fxmlPath + "]");
    activePage = fxmlPath;
    switchScene(event, "/views/" + fxmlPath, title);
  }

  /**
   * Xử lý sự kiện khi click vào mục Dashboard.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  private void handleDashboard(ActionEvent event) {
    navigate(event, "AdminDashboardView.fxml", "Tổng quan hệ thống");
  }

  /**
   * Xử lý sự kiện khi click vào mục Duyệt tài sản.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  private void handleApproveItem(ActionEvent event) {
    navigate(event, "ItemApprovalView.fxml", "Duyệt tài sản");
  }

  /**
   * Xử lý sự kiện khi click vào mục Quản lý người dùng.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  private void handleManageUsers(ActionEvent event) {
    navigate(event, "ManageUserView.fxml", "Quản lý người dùng");
  }

  /**
   * Xử lý sự kiện khi click vào mục Quản lý phiên đấu giá.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  private void handleManageAuctions(ActionEvent event) {
    navigate(event, "ManageAuctionView.fxml", "Quản lý phiên đấu giá");
  }

  /**
   * Áp dụng kiểu dáng (style) đặc trưng riêng làm nổi bật nút đang được chọn.
   *
   * @param btn Đối tượng Button cần được làm nổi bật.
   */
  private void applyActiveStyle(Button btn) {
    if (btn != null) {
      btn.setStyle(
          "-fx-font-weight: bold; -fx-background-color: #e0e7ff; "
              + "-fx-text-fill: #4f46e5; -fx-font-size: 15; -fx-padding: 15 20; "
              + "-fx-cursor: hand; -fx-background-radius: 10;");
    }
  }
}
