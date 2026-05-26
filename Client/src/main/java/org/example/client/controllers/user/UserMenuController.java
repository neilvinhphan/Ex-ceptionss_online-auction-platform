package org.example.client.controllers.user;

import com.google.gson.Gson;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;

import org.example.client.controllers.BaseController;
import org.example.client.controllers.admin.AdminSidebarController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.users.User;
import org.example.core.shared.enums.ActionType;
import org.example.core.shared.enums.RoleType;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller điều khiển thanh menu điều hướng người dùng trên cùng (Top Navigation Bar). Quản lý
 * thông tin hiển thị danh tính tài khoản, phân định màu sắc vai trò (Admin, Seller, Bidder) và xử
 * lý các thao tác đăng xuất hoặc quay lại bảng quản trị hệ thống.
 */
public class UserMenuController extends BaseController implements Initializable {

  @FXML private MenuButton menuUser;
  @FXML private Label roleLabel;
  @FXML private Button btnBackToAdmin;

  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  /**
   * Khởi tạo giao diện, nhận diện người dùng hiện hành từ Session để render tên và phân loại badge
   * chức vụ.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    User currentUser = UserSession.getInstance().getCurrentUser();

    if (currentUser != null) {
      menuUser.setText(currentUser.getUserName());

      if (currentUser.getRole() == RoleType.ADMIN) {
        roleLabel.setText("Admin");
        roleLabel.setStyle("-fx-text-fill: #e63946; -fx-font-size: 11; -fx-font-weight: bold;");

        btnBackToAdmin.setVisible(true);
        btnBackToAdmin.setManaged(true);

      } else if (currentUser.getRole() == RoleType.SELLER) {
        roleLabel.setText("Seller");
        roleLabel.setStyle("-fx-text-fill: #ffc107; -fx-font-size: 11; -fx-font-weight: bold;");
      } else {
        roleLabel.setText("Bidder");
        roleLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11; -fx-font-style: italic;");
      }
    }
  }

  /** Chuyển hướng người dùng về màn hình Trang chủ hệ thống. */
  @FXML
  void handleMain(ActionEvent event) {
    switchScene(event, "/views/MainView.fxml", "Trang chủ");
  }

  /** Chuyển hướng người dùng sang Sảnh sắm sửa / Danh mục sản phẩm công cộng (Catalog View). */
  @FXML
  void handleMenuItem(ActionEvent event) {
    switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục sản phẩm");
  }

  /** Điều hướng người dùng vào trang thiết lập Hồ sơ cá nhân nằm trong khu vực cá nhân. */
  @FXML
  void handleUserUi(ActionEvent event) {
    UserSidebarController.currentView = "PersonalView.fxml";
    switchScene(event, "/views/PersonalView.fxml", "Hồ sơ cá nhân");
  }

  /**
   * Hủy bỏ phiên làm việc hiện tại trong hệ thống (Clear Session) và đẩy người dùng ra màn hình
   * Đăng nhập.
   */
  @FXML
  public void handleLogout(ActionEvent event) {
    try {
      Request logoutRequest = new Request(ActionType.LOGOUT, null);
      String jsonRequest = ClientManager.getInstance().getGson().toJson(logoutRequest);

      String jsonResponse = ClientManager.getInstance().getClient().sendRequest(jsonRequest);
      if (jsonResponse != null) {
        Response response = gson.fromJson(jsonResponse, Response.class);
        if (!"SUCCESS".equals(response.getStatus())) {
          int code = response.getData() instanceof Number ? ((Number) response.getData()).intValue() : -1;
          System.err.println("Server phản hồi lỗi xử lý đăng xuất [" + code + "]: " + response.getMessage());
        }
      }
    } catch (Exception e) {
      System.err.println("Lỗi khi gửi yêu cầu LOGOUT: " + e.getMessage());
    }

    UserSession.getInstance().cleanUserSession();
    switchScene(event, "/views/LoginView.fxml", "Đăng nhập");
  }

  /**
   * Quyền lực Admin: Cho phép quản trị viên rời giao diện sảnh client để quay ngược lại Bảng điều
   * khiển hệ thống Admin Panel.
   */
  @FXML
  void handleBackToAdmin(ActionEvent event) {
    AdminSidebarController.activePage = "AdminDashboardView.fxml";
    switchScene(event, "/views/AdminDashboardView.fxml", "Tổng quan hệ thống - Admin");
  }
}
