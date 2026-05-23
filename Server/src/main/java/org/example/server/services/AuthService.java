package org.example.server.services;

import org.example.core.dto.userDTO.LoginRequestDTO;
import org.example.core.dto.userDTO.RegisterRequestDTO;
import org.example.core.models.users.User;
import org.example.core.shared.enums.UserStatus;
import org.example.server.daos.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
  private final UserDAO userDAO;
  private static volatile AuthService instance = null;

  AuthService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  public static AuthService getInstance() {
    if (instance == null) {
      synchronized (AuthService.class) {
        if (instance == null) {
          instance = new AuthService(UserDAO.getInstance());
        }
      }
    }
    return instance;
  }

  public User register(RegisterRequestDTO requestPayLoad) throws Exception {
    if (requestPayLoad == null) {
      throw new Exception("Dữ liệu đăng ký không được gửi qua biểu mẫu trống!");
    }

    String nameInCheck = requestPayLoad.getUsername();
    String passInCheck = requestPayLoad.getPassword();
    String mailInCheck = requestPayLoad.getEmail();
    String phoneInCheck = requestPayLoad.getPhone();

    // Check rỗng từng trường và ném exception tiếng Việt tương ứng
    if (nameInCheck == null || nameInCheck.trim().isEmpty()) {
      throw new Exception("Vui lòng nhập tên tài khoản (Username)!");
    } else if (passInCheck == null || passInCheck.trim().isEmpty()) {
      throw new Exception("Vui lòng điền mật khẩu đăng nhập!");
    } else if (passInCheck.length() < 6) {
      throw new Exception("Mật khẩu tài khoản phải có độ dài tối thiểu từ 6 ký tự trở lên!");
    } else if (mailInCheck == null || mailInCheck.trim().isEmpty()) {
      throw new Exception("Vui lòng điền thông tin địa chỉ Email!");
    } else if (phoneInCheck == null || phoneInCheck.trim().isEmpty()) {
      throw new Exception("Vui lòng nhập số điện thoại liên lạc!");
    }

    // Check định dạng Email (Dùng Regular Expression)
    String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
    if (!mailInCheck.matches(emailRegex)) {
      throw new Exception("Định dạng thư điện tử không hợp lệ (Ví dụ chính xác: tennguoidung@gmail.com).");
    }

    // Check định dạng Số điện thoại (VN)
    // Quy tắc: Bắt đầu bằng số 0, theo sau là đúng 9 chữ số (tổng 10 số)
    String phoneRegex = "^0\\d{9}$";
    if (!phoneInCheck.matches(phoneRegex)) {
      throw new Exception("Số điện thoại không hợp lệ (Phải bao gồm chính xác 10 chữ số và bắt đầu bằng đầu số 0).");
    }

    // Check trùng username
    User userDB = userDAO.getUserByUsername(nameInCheck);
    if (userDB != null) {
      throw new Exception("Tên tài khoản này đã được sử dụng bởi người khác trong hệ thống!");
    }

    // Hashing password
    String hashedPass = BCrypt.hashpw(passInCheck, BCrypt.gensalt(12));

    // Khởi tạo User (đã pass kiểm duyệt)
    User newUser = new User(nameInCheck, hashedPass, mailInCheck, phoneInCheck);

    boolean isSuccess = userDAO.registerUser(newUser);
    if (!isSuccess) {
      throw new Exception("Đã xảy ra lỗi hệ thống khi đồng bộ lưu trữ thông tin đăng ký vào cơ sở dữ liệu.");
    }

    return newUser;
  }

  public User login(LoginRequestDTO requestPayLoad) throws Exception {
    if (requestPayLoad == null) {
      throw new Exception("Yêu cầu thông tin đăng nhập không hợp lệ hoặc để trống!");
    }

    // Lấy dữ liệu từ giao diện
    String nameInCheck = requestPayLoad.getUsername();
    String passInCheck = requestPayLoad.getPassword();

    // Check rỗng
    if (nameInCheck == null || nameInCheck.trim().isEmpty()) {
      throw new Exception("Tên tài khoản đăng nhập không được bỏ trống.");
    }
    if (passInCheck == null || passInCheck.trim().isEmpty()) {
      throw new Exception("Mật khẩu đăng nhập không được bỏ trống.");
    }

    // Check username
    User userDB = userDAO.getUserByUsername(nameInCheck);
    if (userDB == null) {
      throw new Exception("Tên tài khoản hoặc mật khẩu bạn nhập không chính xác.");
    }

    // Check status
    if (UserStatus.BANNED.equals(userDB.getStatus())) {
      throw new Exception("Tài khoản của bạn đã bị khóa (Ban) hoặc đình chỉ hoạt động bởi ban quản trị hệ thống.");
    }

    // Check password
    if (!(BCrypt.checkpw(passInCheck, userDB.getPassword()))) {
      throw new Exception("Tên tài khoản hoặc mật khẩu bạn nhập không chính xác.");
    }
    return userDB;
  }
}