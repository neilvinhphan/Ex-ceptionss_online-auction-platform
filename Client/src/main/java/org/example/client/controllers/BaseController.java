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
      Stage stage = null;
      Scene currentScene = null;
      Object source = event.getSource();

      // 1. Phân biệt nguồn sự kiện để lấy đúng Cửa sổ (Stage) và Cảnh (Scene)
      if (source instanceof Node) {
        // Nếu bấm từ Button, AnchorPane, VBox...
        currentScene = ((Node) source).getScene();
        stage = (Stage) currentScene.getWindow();
      } else if (source instanceof MenuItem) {
        // Nếu bấm từ MenuItem (MenuItem không kế thừa Node nên phải lấy qua Popup Menu)
        MenuItem menuItem = (MenuItem) source;
        stage = (Stage) menuItem.getParentPopup().getOwnerWindow();
        currentScene = stage.getScene();
      }

      // Kiểm tra an toàn
      if (stage == null || currentScene == null) {
        System.err.println("Lỗi: Không thể xác định được cửa sổ hiện tại!");
        return;
      }

      // Tránh load lại chính trang hiện tại
      if (stage.getTitle() != null && stage.getTitle().equals(title)) {
        return;
      }

      // 2. Tải file FXML giao diện mới
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Parent newRoot = loader.load();

      // 3. MẤU CHỐT GIỮ TỶ LỆ: Chỉ thay đổi Root (ruột) của Scene hiện tại.
      // Tuyệt đối không dùng "stage.setScene(new Scene(newRoot))" vì sẽ làm cửa sổ bị co lại.
      currentScene.setRoot(newRoot);
      stage.setTitle(title);

      // (Bảo hiểm thêm) Ép cửa sổ luôn giữ trạng thái phóng to hết cỡ giống trình duyệt web
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
