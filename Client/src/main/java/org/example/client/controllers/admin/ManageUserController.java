package org.example.client.controllers.admin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.example.client.controllers.BaseController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.admin.AdminBanUserDTO;
import org.example.core.models.users.User;
import org.example.core.shared.enums.ActionType;
import org.example.core.shared.enums.RoleType;
import org.example.core.shared.enums.UserStatus;

/**
 * Controller chịu trách nhiệm hiển thị danh sách người dùng và xử lý các tác vụ khóa/mở khóa tài
 * khoản từ Admin.
 */
public class ManageUserController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(ManageUserController.class.getName());

  @FXML private TextField searchField;
  @FXML private TableView<User> userTable;
  @FXML private TableColumn<User, Integer> colId;
  @FXML private TableColumn<User, String> colUsername;
  @FXML private TableColumn<User, String> colEmail;
  @FXML private TableColumn<User, String> colPhone;
  @FXML private TableColumn<User, RoleType> colRole;
  @FXML private TableColumn<User, UserStatus> colStatus;
  @FXML private TableColumn<User, BigDecimal> colBalance;

  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();
  private final ObservableList<User> userList = FXCollections.observableArrayList();

  /**
   * Thiết lập liên kết dữ liệu giữa cấu trúc bảng và thực thể User, đồng thời thiết lập bộ định
   * dạng tiền tệ cho số dư.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    colId.setCellValueFactory(new PropertyValueFactory<>("userId"));
    colUsername.setCellValueFactory(new PropertyValueFactory<>("userName"));
    colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
    colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
    colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
    colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));

    DecimalFormat formatter = new DecimalFormat("#,###", new DecimalFormatSymbols(new Locale("vi", "VN")));

    colBalance.setCellFactory(
            tc ->
                    new TableCell<User, BigDecimal>() {
                      @Override
                      protected void updateItem(BigDecimal price, boolean empty) {
                        super.updateItem(price, empty);
                        if (empty || price == null) {
                          setText(null);
                        } else {
                          setText(formatter.format(price) + " VNĐ");
                        }
                      }
                    });

    loadUsersFromServer();
  }

  /**
   * Tạo luồng ngầm gửi yêu cầu đồng bộ toàn bộ danh sách thành viên đang có trên hệ thống từ
   * Server.
   */
  private void loadUsersFromServer() {
    new Thread(
            () -> {
              try {
                int adminId = UserSession.getInstance().getCurrentUser().getUserId();
                Request request = new Request(ActionType.ADMIN_GET_ALL_USERS, adminId);
                String requestJson = gson.toJson(request);
                String jsonResponse = clientSocket.sendRequest(requestJson);

                if (jsonResponse != null) {
                  Response response = gson.fromJson(jsonResponse, Response.class);

                  if ("SUCCESS".equals(response.getStatus())) {
                    String dataJson = gson.toJson(response.getData());
                    Type listType = new TypeToken<List<User>>() {}.getType();
                    List<User> users = gson.fromJson(dataJson, listType);

                    Platform.runLater(
                        () -> {
                          userList.setAll(users);
                          userTable.setItems(userList);
                          logger.info("Đã tải xong danh sách người dùng hệ thống thành công.");
                        });
                  } else {
                    logger.warning(
                        "Server từ chối trả về danh sách người dùng: " + response.getMessage());
                    Platform.runLater(() -> showAlert("Lỗi", response.getMessage()));
                  }
                }
              } catch (Exception e) {
                logger.log(
                    Level.SEVERE, "Lỗi mạng hoặc lỗi đồng bộ danh sách người dùng từ máy chủ", e);
                Platform.runLater(
                    () ->
                        showAlert(
                            "Lỗi mạng",
                            "Không thể kết nối đến Server. Chi tiết lỗi: " + e.getMessage()));
              }
            })
        .start();
  }

  /**
   * Điều hướng quay trở về màn hình trang chủ chính của ứng dụng.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  public void handleBack(ActionEvent event) {
    switchScene(event, "/views/MainView.fxml", "Trang chủ");
  }

  /**
   * Thực hiện bộ lọc danh sách thành viên cục bộ dựa trên dữ liệu so khớp Email, Tên tài khoản hoặc
   * Số điện thoại.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  public void handleSearch(ActionEvent event) {
    String keyword = searchField.getText().trim().toLowerCase();
    logger.info("Đang thực hiện tìm kiếm cục bộ người dùng với từ khóa: " + keyword);

    if (keyword.isEmpty()) {
      userTable.setItems(userList);
      return;
    }

    ObservableList<User> filteredList = FXCollections.observableArrayList();
    for (User user : userList) {
      boolean matchUsername =
          user.getUserName() != null && user.getUserName().toLowerCase().contains(keyword);
      boolean matchEmail =
          user.getEmail() != null && user.getEmail().toLowerCase().contains(keyword);
      boolean matchPhone =
          user.getPhone() != null && user.getPhone().toLowerCase().contains(keyword);

      if (matchUsername || matchEmail || matchPhone) {
        filteredList.add(user);
      }
    }

    userTable.setItems(filteredList);

    if (filteredList.isEmpty()) {
      showAlert("Thông báo!", "Không tìm thấy người dùng phù hợp với từ khóa.");
    }
  }

  /**
   * Thiết lập lại trường nhập tìm kiếm và kéo mới lại danh sách người dùng từ phía Server.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  public void handleRefresh(ActionEvent event) {
    searchField.clear();
    if (userTable.getItems() != userList) {
      userTable.setItems(userList);
    }
    loadUsersFromServer();
    logger.info("Đã thực hiện làm mới danh sách tài khoản.");
  }

  /**
   * Thực hiện gửi yêu cầu khóa (Ban) tài khoản người dùng đang được lựa chọn lên hệ thống.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  public void handleBanUser(ActionEvent event) {
    User selectedUser = userTable.getSelectionModel().getSelectedItem();
    if (selectedUser == null) {
      showAlert("Cảnh báo", "Vui lòng chọn một người dùng trong bảng để khóa!");
      return;
    }

    new Thread(() -> {
      try {
        int adminId = UserSession.getInstance().getCurrentUser().getUserId();
        AdminBanUserDTO banDto = new AdminBanUserDTO(adminId, selectedUser.getUserId(), true);
        Request request = new Request(ActionType.ADMIN_BAN_USER, banDto);

        clientSocket.getOut().println(gson.toJson(request));
        String responseStr = clientSocket.getIn().readLine();

        if (responseStr != null) {
          Response response = gson.fromJson(responseStr, Response.class);

          Platform.runLater(() -> {
            if ("SUCCESS".equals(response.getStatus())) {
              showAlert("Thành công", response.getMessage());
              loadUsersFromServer();
            } else {
              handleServerErrorResponse(response); // Gọi hàm tiện ích xử lý lỗi
            }
          });
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Lỗi mạng khi khóa tài khoản", e);
        Platform.runLater(() -> showAlert("Lỗi kết nối", "Chi tiết: " + e.getMessage()));
      }
    }).start();
  }

  /**
   * Thực hiện gửi yêu cầu mở khóa (Unban) tài khoản người dùng đang được lựa chọn lên hệ thống.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  public void handleUnbanUser(ActionEvent event) {
    User selectedUser = userTable.getSelectionModel().getSelectedItem();
    if (selectedUser == null) {
      showAlert("Cảnh báo", "Vui lòng chọn một người dùng trong bảng để mở khóa!");
      return;
    }

    new Thread(() -> {
      try {
        int adminId = UserSession.getInstance().getCurrentUser().getUserId();
        AdminBanUserDTO unbanDto = new AdminBanUserDTO(adminId, selectedUser.getUserId(), false);
        Request request = new Request(ActionType.ADMIN_BAN_USER, unbanDto);

        clientSocket.getOut().println(gson.toJson(request));
        String responseStr = clientSocket.getIn().readLine();

        if (responseStr != null) {
          Response response = gson.fromJson(responseStr, Response.class);

          Platform.runLater(() -> {
            if ("SUCCESS".equals(response.getStatus())) {
              showAlert("Thành công", response.getMessage());
              loadUsersFromServer();
            } else {
              handleServerErrorResponse(response); // Gọi hàm tiện ích xử lý lỗi
            }
          });
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Lỗi mạng khi mở khóa tài khoản", e);
        Platform.runLater(() -> showAlert("Lỗi kết nối", "Chi tiết: " + e.getMessage()));
      }
    }).start();
  }
  /**
   * Hàm tiện ích giúp phân loại mã lỗi trả về từ Server để hiển thị tiêu đề chính xác.
   */
  private void handleServerErrorResponse(Response response) {
    int code = response.getData() instanceof Number ? ((Number) response.getData()).intValue() : -1;
    String errorTitle = switch (code) {
      case 4030 -> "Không đủ quyền hạn (403)";
      case 4040 -> "User không tồn tại (404)";
      case 4090 -> "Xung đột trạng thái (409)";
      case 5000 -> "Lỗi hệ thống Server (500)";
      default -> "Lỗi (" + code + ")";
    };
    logger.warning("Thao tác user thất bại [" + code + "]: " + response.getMessage());
    showAlert(errorTitle, response.getMessage());
  }
}
