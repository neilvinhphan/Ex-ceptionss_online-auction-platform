package org.example.client.controllers;

import com.google.gson.Gson;

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

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuButton;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MainController extends BaseController implements Initializable {
    @FXML
    private HBox upgradeBanner;
    @FXML
    private FlowPane promotedAuctionsContainer;

    // Các thành phần network
    private Gson gson = ClientManager.getInstance().getGson();
    private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = UserSession.getInstance().getCurrentUser();

        if (currentUser != null) {
            // --- KIỂM TRA ROLE ĐỂ ẨN/HIỆN BANNER NÂNG CẤP ---
            // (Không còn xử lý text của Menu ở đây nữa vì MenuController đã lo)
            if (currentUser.getRole() == RoleType.ADMIN || currentUser.getRole() == RoleType.SELLER) {
                // Admin và Seller thì ẩn Banner nâng cấp
                upgradeBanner.setVisible(false);
                upgradeBanner.setManaged(false);
            } else {
                // Mặc định là BIDDER (Người mua) thì hiện Banner nâng cấp
                upgradeBanner.setVisible(true);
                upgradeBanner.setManaged(true);
            }
        }

        // --- GỌI SOCKET LẤY DANH SÁCH ĐỀ CỬ ---
        loadPromotedAuctionsFromServer();
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
        // 7. Xử lý kết quả sau khi nhập
        result.ifPresent(
                password -> {
                    try {
                        User currentUser = UserSession.getInstance().getCurrentUser();
                        int currentId = currentUser.getUserId();

                        System.out.println("--- BẮT ĐẦU KIỂM TRA MẬT KHẨU ---");
                        System.out.println("Pass nhập vào: " + password);
                        System.out.println("Pass trong Session (Hash): " + currentUser.getPassword());

                        // Chặn lỗi nếu Session không lưu password
                        if (currentUser.getPassword() == null || currentUser.getPassword().isEmpty()) {
                            showAlert("Lỗi hệ thống", "Dữ liệu User trong Session không chứa mật khẩu mã hóa. Vui lòng kiểm tra lại API Login phía Server!");
                            return;
                        }

                        if (!BCrypt.checkpw(password, currentUser.getPassword())) {
                            showAlert("Lỗi", "Sai mật khẩu!");
                            return;
                        }

                        System.out.println("=> CHECK BCRYPT THÀNH CÔNG. Chuẩn bị gửi Server...");
                        sendUpgradeRequestToServer(currentId, event);

                    } catch (Exception e) {
                        System.out.println("=> VĂNG LỖI NGẦM TRONG LÚC CHECK BCRYPT:");
                        e.printStackTrace();
                        showAlert("Lỗi vặt", "Có lỗi xảy ra khi kiểm tra mật khẩu: " + e.getMessage());
                    }
                });
    }

    private void sendUpgradeRequestToServer(int userId, ActionEvent event) {
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
                                        UserSession.getInstance().cleanUserSession();
                                        switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
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

    // ================== HÀM LẤY DATA CHO TRANG CHỦ  ==================

    private void loadPromotedAuctionsFromServer() {
        //TODO: LẤY DỮ LIỆU LÊN ĐỂ VẼ CARD, action "GET_PROMOTED_AUCTIONS"
    }
}