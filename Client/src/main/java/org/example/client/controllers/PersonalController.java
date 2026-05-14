package org.example.client.controllers;

import com.google.gson.Gson;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.userDTO.DepositRequestDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.users.User;
import org.example.core.shared.enums.RoleType;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;

public class PersonalController extends BaseController implements Initializable {

    @FXML
    private Label lbUserName;
    @FXML
    private Label lbPhoneNum;
    @FXML
    private Label lbEmail;
    @FXML
    private Label lbPassWord;
    @FXML
    private Label lblRole;
    @FXML
    private Label lbBalance;
    RoleType userRole = UserSession.getInstance().getCurrentUser().getRole();
    private Gson gson = ClientManager.getInstance().getGson();
    private final AuctionClient clientSocket = ClientManager.getInstance().getClient();
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

    @FXML
    private Button createAuction;
    @FXML
    public void handleHistoryAuction(ActionEvent event) {
    switchScene(event, "/views/AuctionHistoryView.fxml", "Lich su dau gia");
    }
    @FXML
    void handleCreateAuction(ActionEvent event) {
        if (userRole.equals(RoleType.BIDDER)) {
            showAlert("Không có quyền", "Chỉ người mua mới có thể tạo cuộc đấu giá!");
        } else {
            switchScene(event, "/views/CreateAuctionView.fxml", "Tạo cuộc đấu giá");
        }
    }

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
            int currentUserId = currentUser.getUserId();
            if (true) {
                currentUser.setPhone(newPhone);
                lbPhoneNum.setText(newPhone);
                showAlert("Thành công", "Đã cập nhật số điện thoại mới vào Database!");
            } else {
                showAlert("Lỗi", "Không thể cập nhật số điện thoại vào Database!");
            }
        }
    }

    @FXML
    void handleEditEmail(ActionEvent event) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        TextInputDialog dialog = new TextInputDialog(currentUser.getEmail());
        dialog.setTitle("Cập nhật thông tin");
        dialog.setHeaderText("Chỉnh sửa Email");
        dialog.setContentText("Nhập Email mới của bạn:");
        // Lấy kết quả người dùng nhập
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String newEmail = result.get().trim();
            int currentUserId = currentUser.getUserId();
            if (true) {
                currentUser.setEmail(newEmail);
                lbEmail.setText(newEmail);
                showAlert("Thành công", "Đã cập nhật số điện thoại mới vào Database!");
            } else {
                showAlert("Lỗi", "Không thể cập nhật số điện thoại vào Database!");
            }
        }
    }

    @FXML
    void handleEditPassWord(ActionEvent event) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        TextInputDialog dialog = new TextInputDialog(currentUser.getPassword());
        dialog.setTitle("Cập nhật thông tin");
        dialog.setHeaderText("Chỉnh sửa mật khẩu");
        dialog.setContentText("Nhập mật khẩu mới của bạn:");
        // Lấy kết quả người dùng nhập
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String newPassWord = result.get().trim();
            int currentUserId = currentUser.getUserId();
            if (true) {
                currentUser.setEmail(newPassWord);
                lbPassWord.setText(newPassWord);
                showAlert("Thành công", "Đã cập nhật mật khẩu mới vào Database!");
            } else {
                showAlert("Lỗi", "Không thể cập nhật mật khẩu vào Database!");
            }
        }
    }

    @FXML
    void handleCreateItem(ActionEvent event) {
        if (userRole.equals(RoleType.SELLER)) {
            switchScene(event, "/views/CreateItemView.fxml", "Tạo sản phẩm đấu giá");
        } else {
            showAlert("Không có quyền", "Chỉ người mua mới có thể tạo sản phẩm đấu giá!");
        }
    }

    @FXML
    void handleWareHouse(ActionEvent event) {
        switchScene(event, "/views/WareHouseView.fxml", "Kho hàng");
    }
    @FXML
    public void handleWaitPayment(ActionEvent event) {
        switchScene(event, "/views/WaitPaymentView.fxml", "San pham cho thanh toan");
    }

    @FXML
    public void handleDeposit(ActionEvent event) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // 1. Tạo Dialog nhập liệu (Giữ nguyên đoạn UI của bạn)
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Nạp tiền");
        dialog.setHeaderText("Nhập số tiền và mật khẩu xác nhận");

        ButtonType confirmButton = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField tfAmount = new TextField();
        PasswordField pfPassword = new PasswordField();

        grid.add(new Label("Số tiền:"), 0, 0);
        grid.add(tfAmount, 1, 0);
        grid.add(new Label("Mật khẩu:"), 0, 1);
        grid.add(pfPassword, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> btn == confirmButton ? new String[]{tfAmount.getText(), pfPassword.getText()} : null);

        dialog.showAndWait().ifPresent(data -> {
            String inputAmount = data[0];
            String inputPassword = data[1];

            // ĐÃ XÓA SẠCH BCrypt VÀ CÁC DÒNG GSON THỪA THÃI Ở ĐÂY!
            // Nhiệm vụ của Frontend là gửi mật khẩu thô lên Server, Server sẽ tự lo.

            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(inputAmount);
                if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    showAlert("Lỗi", "Số tiền phải lớn hơn 0!");
                    return;
                }

                // LƯU Ý BƯỚC NÀY: Bạn phải mở class DepositRequestDTO ra và thêm thuộc tính password vào đó
                DepositRequestDTO dto = new DepositRequestDTO(currentUser.getUserId(), amount, inputPassword);
                sendDepositToServer(dto);

            } catch (NumberFormatException e) {
                showAlert("Lỗi", "Số tiền không hợp lệ!");
            }
        });
    }

    private void sendDepositToServer(DepositRequestDTO dto) {
        Request req = new Request("DEPOSIT", dto);
        String jsonReq = gson.toJson(req);

        new Thread(() -> {
            try {
                String jsonRes = clientSocket.sendRequest(jsonReq);
                System.out.println("DEBUG - Raw Response from Server: >>>" + jsonRes + "<<<");

                Response res = gson.fromJson(jsonRes.trim(), Response.class);

                Platform.runLater(() -> {
                    if ("SUCCESS".equals(res.getStatus())) {
                        Object data = res.getData();
                        if (data != null) {
                            BigDecimal newBalance = new BigDecimal(data.toString());

                            // Cập nhật Session
                            UserSession.getInstance().getCurrentUser().setBalance(newBalance);

                            // FIX LỖI CRASH Ở ĐÂY: newBalance.toPlainString() là chuỗi,
                            // dùng format %,.0f sẽ ném Exception. Đã sửa lại thành newBalance.
                            lbBalance.setText(String.format("%,.0f", newBalance));
                            showAlert("Thành công", "Nạp tiền thành công!\nSố dư mới: " + String.format("%,.0f", newBalance) + " VND");
                        }
                    } else {
                        showAlert("Thất bại", res.getMessage()); // Hiển thị lỗi sai mật khẩu từ Server
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối", "Lỗi dữ liệu từ Server!"));
                e.printStackTrace();
            }
        }).start();
    }
}
