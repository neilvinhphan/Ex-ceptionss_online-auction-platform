package org.example.client.controllers;

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

    protected void PasswordDisplayLogic(PasswordField passHidden, TextField passShow) {
        if (passHidden.isVisible()) {
            passShow.setText(passHidden.getText());
            passShow.setVisible(true);
            passHidden.setVisible(false);
        } else {
            passHidden.setText(passShow.getText());
            passHidden.setVisible(true);
            passShow.setVisible(false);
        }
    }

    protected void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Stage stage = null;
            Scene currentScene = null;
            Object source = event.getSource();
            if (source instanceof Node) {
                currentScene = ((Node) source).getScene();
                stage = (Stage) currentScene.getWindow();
            } else if (source instanceof MenuItem) {
                MenuItem menuItem = (MenuItem) source;
                stage = (Stage) menuItem.getParentPopup().getOwnerWindow();
                currentScene = stage.getScene();
            }
            if (stage == null || currentScene == null) {
                System.err.println("Lỗi: Không thể xác định được cửa sổ hiện tại!");
                return;
            }
            if (stage.getTitle() != null && stage.getTitle().equals(title)) {
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newRoot = loader.load();
            currentScene.setRoot(newRoot);
            stage.setTitle(title);
            stage.setMaximized(true);

        } catch (IOException e) {
            System.err.println("Lỗi chuyển cảnh sang " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
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
