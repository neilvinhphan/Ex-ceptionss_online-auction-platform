package org.example.client.controllers;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller nền tảng (Base Controller) chứa các phương thức tiện ích dùng chung. Cung cấp giải
 * pháp chuyển cảnh (Scene Switching) tối ưu, hiển thị hộp thoại thông báo (Alert) và logic ẩn/hiện
 * mật khẩu dùng chung cho toàn bộ các màn hình phía Client.
 */
public class BaseController {

  private static final Logger logger = Logger.getLogger(BaseController.class.getName());

  /**
   * Xử lý chuyển đổi cảnh (Scene) linh hoạt trong cùng một cửa sổ (Stage). Hỗ trợ tự động nhận diện
   * Stage từ cả Node (Button, TextField...) và MenuItem (Context Menu/Menu Bar).
   *
   * @param event Sự kiện hành động kích hoạt từ giao diện.
   * @param fxmlPath Đường dẫn tương đối dẫn tới file giao diện FXML đích.
   * @param title Tiêu đề hiển thị mới cho cửa sổ hệ thống.
   */
  protected void switchScene(ActionEvent event, String fxmlPath, String title) {
    try {
      Stage stage = null;
      Scene currentScene = null;
      Object source = event.getSource();

      if (source instanceof Node) {
        currentScene = ((Node) source).getScene();
        if (currentScene != null) {
          stage = (Stage) currentScene.getWindow();
        } else {
          logger.warning("Lỗi: Node source chưa được gắn vào một Scene cụ thể.");
          return;
        }
      } else if (source instanceof MenuItem) {
        MenuItem menuItem = (MenuItem) source;
        stage = (Stage) menuItem.getParentPopup().getOwnerWindow();
        currentScene = stage.getScene();
      }

      if (stage == null || currentScene == null) {
        logger.warning("Lỗi: Không thể xác định được cửa sổ Stage hoặc Scene hiện tại!");
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
      logger.log(
          Level.SEVERE,
          "Gặp lỗi nghiêm trọng khi cố gắng tải FXML và chuyển cảnh sang: " + fxmlPath,
          e);
    }
  }

  /**
   * Hiển thị một hộp thoại thông báo thông tin (Information Alert) tiêu chuẩn cho người dùng.
   *
   * @param title Tiêu đề nằm trên thanh khung của hộp thoại.
   * @param content Nội dung thông điệp chi tiết muốn truyền tải.
   */
  protected void showAlert(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  /**
   * Xử lý logic hoán đổi đồng bộ chuỗi ký tự giữa PasswordField (ẩn) và TextField (hiện văn bản
   * thuần).
   *
   * @param passHidden Thành phần PasswordField ẩn giấu mật khẩu gốc.
   * @param passShow Thành phần TextField hiển thị mật khẩu công khai.
   */
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
}
