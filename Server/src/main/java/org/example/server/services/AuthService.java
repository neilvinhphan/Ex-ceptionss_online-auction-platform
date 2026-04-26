package org.example.server.services;

import org.example.core.dto.LoginRequestDTO;
import org.example.core.dto.RegisterRequestDTO;
import org.example.core.models.users.User;
import org.example.server.daos.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
  protected static UserDAO userDAO = UserDAO.getInstance();

  public static User register(RegisterRequestDTO requestPayLoad) throws Exception {

    String nameInCheck = requestPayLoad.getUsername();
    String passInCheck = requestPayLoad.getPassword();
    String rePassword = requestPayLoad.getRePassword();
    String passInCheckHide = requestPayLoad.getPassword();
    String rePasswordHide = requestPayLoad.getRePassword();
    String mailInCheck = requestPayLoad.getEmail();
    String phoneInCheck = requestPayLoad.getPhone();

    // Check rỗng
    if (nameInCheck == null || nameInCheck.trim().isEmpty()) {
      throw new Exception("Please enter an username");
    } else if (passInCheck == null || passInCheck.trim().isEmpty()) {
      throw new Exception("Please enter a password");
    } else if (mailInCheck == null || mailInCheck.trim().isEmpty()) {
      throw new Exception("Please enter an email");
    } else if (phoneInCheck == null || phoneInCheck.trim().isEmpty()) {
      throw new Exception("Please enter a phone number");
    }

    // Check định dạng Email (Dùng Regular Expression)
    String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
    if (!mailInCheck.matches(emailRegex)) {
      throw new Exception("Invalid email format (e.g., example@gmail.com).");
    }

    // Check định dạng Số điện thoại (VN)
    // Quy tắc: Bắt đầu bằng số 0, theo sau là đúng 9 chữ số (tổng 10 số)
    String phoneRegex = "^0\\d{9}$";
    if (!phoneInCheck.matches(phoneRegex)) {
      throw new Exception("Invalid phone number format (must be 10 digits starting with 0).");
    }

    // Check password & repassword
    if (!passInCheck.equals(rePassword) || (!passInCheckHide.equals(rePasswordHide))) {
      throw new Exception("Passwords do not matched.");
    }

    // Check Terms and Conditions tick
    boolean tickCheck = requestPayLoad.isTickCheck();
    if (!tickCheck) {
      throw new Exception("Please accept the terms and conditions to proceed");
    }

    // Check username
    User userDB = userDAO.getUserByUsername(nameInCheck);
    if (userDB != null) {
      throw new Exception("Username existed.");
    }

    // Hashing password
    String hashedPass = BCrypt.hashpw(passInCheck, BCrypt.gensalt(12));

    // Khởi tạo User (đã pass kiểm duyệt)
    User newUser = new User(nameInCheck, hashedPass, mailInCheck, phoneInCheck);

    boolean isSuccess = userDAO.registerUser(newUser);
    if (!isSuccess) {
      throw new Exception("Something went wrong in DB.");
    }

    return newUser;
  }

  public static User login(LoginRequestDTO requestPayLoad) throws Exception {

    // Lấy dữ liệu từ giao diện
    String nameInCheck = requestPayLoad.getUsername();
    String passInCheck = requestPayLoad.getPassword();

    // Check rỗng
    if (nameInCheck == null || nameInCheck.trim().isEmpty()) {
      throw new Exception("Please enter the username.");
    }

    // Check username
    User userDB = userDAO.getUserByUsername(nameInCheck);
    if (userDB == null) {
      throw new Exception("Wrong username or password.");
    }

    // Check status
    if (userDB.getStatus().equals("BANNED")) {
      throw new Exception("Your accound has banned.");
    }

    // Check password
    if (!(BCrypt.checkpw(passInCheck, userDB.getPassword()))) {
      throw new Exception("Wrong username or password.");
    }
    return userDB;
  }
}
