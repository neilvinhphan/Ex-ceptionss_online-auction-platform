package org.example.server.client.controllers;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController extends BaseController {
    @FXML
    private TextField tfuserName;
    @FXML
    private TextField tfphone;
    @FXML
    private TextField tfemail;
    @FXML
    private TextField pass_hien;
    @FXML
    private TextField repass_hien;
    @FXML
    private PasswordField pass_an;
    @FXML
    private PasswordField repass_an;
@FXML
private CheckBox cbCommit;
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    void handleRegister(ActionEvent event){
        String userName = tfuserName.getText();
        String phone = tfphone.getText();
        String email = tfemail.getText();
        String password = pass_an.getText();
        String passwordhidden = pass_hien.getText();
        String repassword = repass_an.getText();
        String repasswordhidden = repass_hien.getText();
        if (userName.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty() || repassword.isEmpty()){
            showAlert("Lỗi","Vui lòng nhập đủ thông tin!");
        }
        if(!password.equals(repassword)){
            showAlert("Lỗi","Mật khẩu không khớp! Vui lòng kiểm tra lại! ");
        }
        if(!cbCommit.isSelected()){
            showAlert("Thông báo","Vui lòng đồng ý với điều khoản dịch vụ để tiếp tục!");
        }
        // sẽ có 1 cái gì đó check xem trong database tồn tại tài kh đó chưa;)) t chưa nghĩ ra
        else{
            System.out.println("Chuyển sang trang Login");
        }
    }
    void hienthi_pass(ActionEvent event){
        logichienthi_pass(pass_an,pass_hien);
    }
    void rehienthi_pass(ActionEvent event){
        logichienthi_pass(repass_an,repass_hien);
    }
    void handleLogin(ActionEvent event){
        System.out.println("Sang trang login");
        switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
    }
}
