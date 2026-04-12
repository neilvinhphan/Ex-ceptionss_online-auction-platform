package org.example.server.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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
            // 1. Tải file FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();

            // 2. Lấy Stage hiện tại
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

            // 3. Hiển thị Scene mới
            stage.setTitle(title);
            stage.setScene(new javafx.scene.Scene(root));
            stage.setMaximized(true); // Giữ full màn hình
            stage.show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi rồi: " + e.getMessage());
        }
    }
}
