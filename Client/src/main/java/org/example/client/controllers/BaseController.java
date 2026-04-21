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
  protected boolean isPasswordVisible = false;

  protected void PasswordDisplayLogic(PasswordField pass_an, TextField pass_hien) {
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
      Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      Scene currentScene = ((Node) event.getSource()).getScene();
      if (stage.getTitle() != null && stage.getTitle().equals(title)) {
        return;
      }
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Parent newRoot = loader.load();
      currentScene.setRoot(newRoot);
      stage.setTitle(title);
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
