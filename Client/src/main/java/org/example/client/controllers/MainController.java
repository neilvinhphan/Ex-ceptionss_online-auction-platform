package org.example.client.controllers;

import com.google.gson.Gson;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.UpdateRoleRequestDTO;
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
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MainController extends BaseController implements Initializable {
    @FXML private HBox upgradeBanner;
    @FXML private FlowPane promotedAuctionsContainer;

    // Các thành phần network
    private Gson gson = ClientManager.getInstance().getGson();
    private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

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

    @FXML
    private void handleUpgrade(ActionEvent event) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Xác nhận nâng cấp");
        dialog.setHeaderText("Bạn có chắc chắn muốn nâng cấp lên tài khoản SELLER không?\nNếu có, vui lòng nhập lại mật khẩu để xác nhận:");

        ButtonType confirmButtonType = new ButtonType("Nâng cấp", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Nhập mật khẩu hiện tại của bạn...");

        VBox vbox = new VBox();
        vbox.setSpacing(10);
        vbox.getChildren().add(passwordField);
        dialog.getDialogPane().setContent(vbox);

        Platform.runLater(passwordField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return passwordField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(password -> {
            User currentUser = UserSession.getInstance().getCurrentUser();
            int currentId = currentUser.getUserId();

            if (!BCrypt.checkpw(password, currentUser.getPassword())) {
                showAlert("Lỗi", "Sai mật khẩu!");
                return;
            } else if (password.trim().isEmpty()) {
                showAlert("Cảnh báo", "Mật khẩu không được để trống!");
                return;
            }

            System.out.println("Gửi request nâng cấp với mật khẩu: " + password);
            sendUpgradeRequestToServer(currentId);
        });
    }

    private void sendUpgradeRequestToServer(int userId) {
        UpdateRoleRequestDTO updateRoleRequestDTO = new UpdateRoleRequestDTO(userId);
        Request request = new Request("UPDATE_ROLE", updateRoleRequestDTO);
        String jsonRequest = gson.toJson(request);

        new Thread(() -> {
            try {
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        showAlert("Chúc mừng", "Đăng ký thành công! Vui lòng đăng nhập lại để hệ thống cập nhật phiên làm việc.");
                        // Force logout để user đăng nhập lại lấy Role mới
                        UserSession.getInstance().cleanUserSession();
                        switchScene(new ActionEvent(), "/views/LoginView.fxml", "Đăng nhập hệ thống");
                    } else {
                        showAlert("Đã xảy ra lỗi", "Không thể nâng cấp lúc này, vui lòng thử lại sau.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối", "Không thể gửi yêu cầu: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    // ================== HÀM LẤY DATA CHO TRANG CHỦ ==================

    private void loadPromotedAuctionsFromServer() {
        // Gửi request lấy list đấu giá đề cử
        Request request = new Request("GET_PROMOTED_AUCTIONS", null);
        String jsonRequest = gson.toJson(request);

        new Thread(() -> {
            try {
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);

                Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        // TODO: Parse response.getPayload() thành List<Auction> và nhét vào promotedAuctionsContainer
                        System.out.println("Lấy danh sách đề cử thành công!");
                        // buildPromotedCards(list);
                    }
                });
            } catch (Exception e) {
                System.out.println("Lỗi khi tải danh sách đề cử: " + e.getMessage());
            }
        }).start();
    }
}