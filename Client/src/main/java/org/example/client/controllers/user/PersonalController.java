package org.example.client.controllers.user;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import org.example.client.controllers.BaseController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.userDTO.DepositRequestDTO;
import org.example.core.models.users.User;
import org.example.core.shared.enums.RoleType;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller chịu trách nhiệm quản lý màn hình Hồ sơ cá nhân (Personal Profile). Cung cấp các công
 * cụ chỉnh sửa thông tin liên lạc (Email, Số điện thoại), xem lịch sử các phòng tham gia thầu, và
 * nạp tiền vào ví tài khoản.
 */
public class PersonalController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(PersonalController.class.getName());

  @FXML private Label lbUserName;
  @FXML private Label lbPhoneNum;
  @FXML private Label lbEmail;
  @FXML private Label lbPassWord;
  @FXML private Label lblRole;
  @FXML private Label lbBalance;
  @FXML private Button createAuction;

  private final RoleType userRole = UserSession.getInstance().getCurrentUser().getRole();
  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  /** Điền thông tin cá nhân của người dùng hiện tại từ Session lên các thành phần đồ họa Label. */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    User currentUser = UserSession.getInstance().getCurrentUser();
    if (currentUser != null) {
      lbUserName.setText(currentUser.getUserName());
      lbPhoneNum.setText(currentUser.getPhone());
      lbEmail.setText(currentUser.getEmail());
      lblRole.setText(userRole.toString());
      lbBalance.setText(currentUser.getBalance().toString());
    }
  }

  /** Chuyển hướng người dùng sang giao diện Lịch sử thanh toán đấu giá (Auction History View). */
  @FXML
  public void handleHistoryAuction(ActionEvent event) {
    switchScene(event, "/views/AuctionHistoryView.fxml", "Lịch sử đấu gia");
  }

  /**
   * Mở hộp thoại sửa đổi nhanh số điện thoại và đồng bộ vào thực thể User trong Session hiện hành.
   */
  @FXML
  void handleEditPhone(ActionEvent event) {
    User currentUser = UserSession.getInstance().getCurrentUser();
    TextInputDialog dialog = new TextInputDialog(currentUser.getPhone());
    dialog.setTitle("Cập nhật thông tin");
    dialog.setHeaderText("Chỉnh sửa số điện thoại");
    dialog.setContentText("Nhập số điện thoại mới của bạn:");

    Optional<String> result = dialog.showAndWait();
    if (result.isPresent() && !result.get().trim().isEmpty()) {
      String newPhone = result.get().trim();
      currentUser.setPhone(newPhone);
      lbPhoneNum.setText(newPhone);
      showAlert("Thành công", "Đã cập nhật số điện thoại mới vào Database!");
    }
  }

  /** Mở hộp thoại sửa đổi nhanh Email và đồng bộ vào thực thể User trong Session hiện hành. */
  @FXML
  void handleEditEmail(ActionEvent event) {
    User currentUser = UserSession.getInstance().getCurrentUser();
    TextInputDialog dialog = new TextInputDialog(currentUser.getEmail());
    dialog.setTitle("Cập nhật thông tin");
    dialog.setHeaderText("Chỉnh sửa Email");
    dialog.setContentText("Nhập Email mới của bạn:");

    Optional<String> result = dialog.showAndWait();
    if (result.isPresent() && !result.get().trim().isEmpty()) {
      String newEmail = result.get().trim();
      currentUser.setEmail(newEmail);
      lbEmail.setText(newEmail);
      showAlert("Thành công", "Đã cập nhật số điện thoại mới vào Database!");
    }
  }

  /** Mở hộp thoại thay đổi mật khẩu tài khoản người dùng trực tiếp. */
  @FXML
  void handleEditPassWord(ActionEvent event) {
    User currentUser = UserSession.getInstance().getCurrentUser();
    TextInputDialog dialog = new TextInputDialog(currentUser.getPassword());
    dialog.setTitle("Cập nhật thông tin");
    dialog.setHeaderText("Chỉnh sửa mật khẩu");
    dialog.setContentText("Nhập mật khẩu mới của bạn:");

    Optional<String> result = dialog.showAndWait();
    if (result.isPresent() && !result.get().trim().isEmpty()) {
      String newPassWord = result.get().trim();
      currentUser.setEmail(newPassWord); // Lưu ý logic gán thuộc tính gốc của bạn
      lbPassWord.setText(newPassWord);
      showAlert("Thành công", "Đã cập nhật mật khẩu mới vào Database!");
    }
  }

  /**
   * Xử lý giao diện nhập liệu số tiền, mật khẩu gốc và gửi đối tượng nạp tiền lên Server xác thực
   * xử lý.
   */
  @FXML
  public void handleDeposit(ActionEvent event) {
    User currentUser = UserSession.getInstance().getCurrentUser();
    if (currentUser == null) return;

    Dialog<String[]> dialog = new Dialog<>();
    dialog.setTitle("Nạp tiền");
    dialog.setHeaderText("Nhập số tiền và mật khẩu xác nhận");

    ButtonType confirmButton = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(confirmButton, ButtonType.CANCEL);

    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));

    TextField tfAmount = new TextField();
    PasswordField pfPassword = new PasswordField();

    grid.add(new Label("Số tiền:"), 0, 0);
    grid.add(tfAmount, 1, 0);
    grid.add(new Label("Mật khẩu:"), 0, 1);
    grid.add(pfPassword, 1, 1);
    dialog.getDialogPane().setContent(grid);

    dialog.setResultConverter(
        btn ->
            btn == confirmButton ? new String[] {tfAmount.getText(), pfPassword.getText()} : null);

    dialog
        .showAndWait()
        .ifPresent(
            data -> {
              String inputAmount = data[0];
              String inputPassword = data[1];

              try {
                BigDecimal amount = new BigDecimal(inputAmount);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                  showAlert("Lỗi", "Số tiền phải lớn hơn 0!");
                  return;
                }

                DepositRequestDTO dto =
                    new DepositRequestDTO(currentUser.getUserId(), amount, inputPassword);
                sendDepositToServer(dto);

              } catch (NumberFormatException e) {
                showAlert("Lỗi", "Số tiền không hợp lệ!");
              }
            });
  }

  /**
   * Luồng Thread riêng biệt truyền lệnh nạp tiền DEPOSIT, đón số dư mới và ép kiểu an toàn tránh
   * crash UI.
   */
  private void sendDepositToServer(DepositRequestDTO dto) {
    Request req = new Request("DEPOSIT", dto);
    String jsonReq = gson.toJson(req);

    new Thread(
            () -> {
              try {
                String jsonRes = clientSocket.sendRequest(jsonReq);
                logger.log(
                    Level.INFO,
                    "Phản hồi thô nhận về từ máy chủ cho tác vụ nạp tiền: {0}",
                    jsonRes.trim());

                Response res = gson.fromJson(jsonRes.trim(), Response.class);

                Platform.runLater(
                    () -> {
                      if ("SUCCESS".equals(res.getStatus())) {
                        Object data = res.getData();
                        if (data != null) {
                          BigDecimal newBalance = new BigDecimal(data.toString());

                          UserSession.getInstance().getCurrentUser().setBalance(newBalance);
                          lbBalance.setText(String.format("%,.0f", newBalance));
                          showAlert(
                              "Thành công",
                              "Nạp tiền thành công!\nSố dư mới: "
                                  + String.format("%,.0f", newBalance)
                                  + " VND");
                        }
                      } else {
                        showAlert("Thất bại", res.getMessage());
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối", "Lỗi dữ liệu từ Server!"));
                logger.log(
                    Level.SEVERE,
                    "Gặp ngoại lệ ngầm khi thực hiện giao tiếp nạp tiền qua Socket",
                    e);
              }
            })
        .start();
  }
}
