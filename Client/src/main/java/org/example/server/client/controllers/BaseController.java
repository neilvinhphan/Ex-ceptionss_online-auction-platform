package org.example.server.client.controllers;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class BaseController {

    protected boolean isPasswordVisible = false;

    protected void logichienthi_pass(PasswordField pass_an, TextField pass_hien) {
        if (!isPasswordVisible) {
            pass_hien.setText(pass_an.getText());
            pass_hien.setVisible(true);
            pass_an.setVisible(false);
            isPasswordVisible = true;
        } else {
            pass_an.setText(pass_hien.getText());
            pass_an.setVisible(true);
            pass_hien.setVisible(false);
            isPasswordVisible = false;

        }
    }

    protected void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            // 1. Lấy Stage hiện tại
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 2. KIỂM TRA: Nếu tiêu đề hiện tại đã trùng với tiêu đề trang muốn tới
            if (stage.getTitle() != null && stage.getTitle().equals(title)) {
                System.out.println("Bạn đang ở trang " + title + "rồi, không chuyển nữa.");
                return; // Dừng hàm tại đây, không load FXML nữa
            }

            // 3. Nếu chưa ở trang đó, tiến hành load Scene mới
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi chuyển cảnh sang " + fxmlPath + ": " + e.getMessage());
        }
    }

    protected void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}
