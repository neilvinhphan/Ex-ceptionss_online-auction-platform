package org.example.client.controllers.user;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;

import org.example.client.controllers.BaseController;
import org.example.client.utils.UserSession;
import org.example.core.models.users.User;
import org.example.core.shared.enums.RoleType;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller vận hành thanh chức năng bên cạnh (Sidebar) thuộc bảng điều khiển cá nhân. Kiểm soát
 * trạng thái tô màu highlight của nút bấm đang được mở thông qua biến tĩnh và quản lý màng lọc phân
 * quyền truy cập nghiêm ngặt đối với các tính năng đặc thù của nhóm người bán (Seller).
 */
public class UserSidebarController extends BaseController implements Initializable {

  public static String currentView = "PersonalView.fxml";

  @FXML private Button btnProfile;
  @FXML private Button btnWaitPayment;
  @FXML private Button btnWarehouse;
  @FXML private Button btnHistory;
  @FXML private Button btnCreateItem;
  @FXML private Button btnCreateAuction;
  @FXML private Button btnRevenue;

  /**
   * Đồng bộ hóa giao diện ngay khi Sidebar được nạp lên bằng cách quét và tô màu CSS cho tab đang
   * hoạt động.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    applyActiveStyle();
  }

  @FXML
  private void handleUserUi(ActionEvent event) {
    navigate(event, "PersonalView.fxml", "Hồ sơ", false);
  }

  @FXML
  private void handleWaitPayment(ActionEvent event) {
    navigate(event, "WaitPaymentView.fxml", "Thanh toán", false);
  }

  @FXML
  private void handleWareHouse(ActionEvent event) {
    navigate(event, "WareHouseView.fxml", "Kho hàng", true);
  }

  @FXML
  private void handleHistoryAuction(ActionEvent event) {
    navigate(event, "AuctionHistoryView.fxml", "Lịch sử", false);
  }

  @FXML
  private void handleCreateItem(ActionEvent event) {
    navigate(event, "CreateItemView.fxml", "Tạo sản phẩm", true);
  }

  @FXML
  private void handleCreateAuction(ActionEvent event) {
    navigate(event, "CreateAuctionView.fxml", "Tạo đấu giá", true);
  }

  @FXML
  public void handleRevenue(ActionEvent event) {
    navigate(event, "RevenueView.fxml", "Doanh thu", true);
  }

  /**
   * Phương thức điều hướng thông minh tập trung: Chặn tự kích hoạt lại khi nhấn trúng tab đang
   * đứng, kiểm tra role hợp lệ và cập nhật CSS Sidebar động.
   */
  private void navigate(ActionEvent event, String fxmlPath, String title, boolean requireSeller) {
    if (currentView.equals(fxmlPath)) return;

    if (requireSeller) {
      User user = UserSession.getInstance().getCurrentUser();
      if (user == null || (user.getRole() != RoleType.SELLER)) {
        showAlert("Quyền truy cập", "Tính năng này chỉ dành cho SELLER!");
        return;
      }
    }

    currentView = fxmlPath;
    applyActiveStyle();
    switchScene(event, "/views/" + fxmlPath, title);
  }

  /**
   * Quét dọn toàn bộ danh sách nút bấm, gỡ bỏ class style cũ và ghim class "sidebar-active" vào
   * đúng đích đến hiện tại.
   */
  private void applyActiveStyle() {
    for (Node node :
        new Node[] {
          btnProfile,
          btnWaitPayment,
          btnWarehouse,
          btnHistory,
          btnCreateItem,
          btnCreateAuction,
          btnRevenue
        }) {
      if (node != null) {
        node.getStyleClass().remove("sidebar-active");
      }
    }

    if (currentView == null || currentView.isEmpty()) return;

    switch (currentView) {
      case "PersonalView.fxml" -> {
        if (btnProfile != null) btnProfile.getStyleClass().add("sidebar-active");
      }
      case "WaitPaymentView.fxml" -> {
        if (btnWaitPayment != null) btnWaitPayment.getStyleClass().add("sidebar-active");
      }
      case "WareHouseView.fxml" -> {
        if (btnWarehouse != null) btnWarehouse.getStyleClass().add("sidebar-active");
      }
      case "AuctionHistoryView.fxml" -> {
        if (btnHistory != null) btnHistory.getStyleClass().add("sidebar-active");
      }
      case "CreateItemView.fxml" -> {
        if (btnCreateItem != null) btnCreateItem.getStyleClass().add("sidebar-active");
      }
      case "CreateAuctionView.fxml" -> {
        if (btnCreateAuction != null) btnCreateAuction.getStyleClass().add("sidebar-active");
      }
      case "RevenueView.fxml" -> {
        if (btnRevenue != null) btnRevenue.getStyleClass().add("sidebar-active");
      }
      default -> {
        if (btnProfile != null) btnProfile.getStyleClass().add("sidebar-active");
      }
    }
  }
}
