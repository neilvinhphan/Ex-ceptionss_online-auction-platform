package org.example.server.services;

import org.example.core.dto.userDTO.LoginRequestDTO;
import org.example.core.dto.userDTO.RegisterRequestDTO;
import org.example.core.models.users.User;
import org.example.core.shared.enums.UserStatus;
import org.example.server.daos.UserDAO;
import org.mindrot.jbcrypt.BCrypt;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dịch vụ quản lý xác thực tài khoản: Tiếp nhận xử lý nghiệp vụ đăng ký thành viên mới và kiểm tra
 * thông tin đăng nhập hệ thống.
 */
public class AuthService {
  private static final Logger logger = Logger.getLogger(AuthService.class.getName());
  private static volatile AuthService instance = null;
  private final UserDAO userDAO;

  AuthService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  /** Lấy instance duy nhất (Singleton) của AuthService (Thread-safe). */
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

  /**
   * Tiếp nhận biểu mẫu, kiểm tra định dạng dữ liệu ràng buộc và tiến hành mã hóa lưu trữ đăng ký
   * User mới.
   */
  public User register(RegisterRequestDTO requestPayLoad) throws Exception {
    if (requestPayLoad == null) {
      throw new IllegalArgumentException("Dữ liệu đăng ký không được gửi qua biểu mẫu trống!");
    }

    String nameInCheck = requestPayLoad.getUsername();
    String passInCheck = requestPayLoad.getPassword();
    String mailInCheck = requestPayLoad.getEmail();
    String phoneInCheck = requestPayLoad.getPhone();

    validateRegisterFields(nameInCheck, passInCheck, mailInCheck, phoneInCheck);

    // Xác thực logic nghiệp vụ trùng lặp tài khoản
    User userDB = userDAO.getUserByUsername(nameInCheck);
    if (userDB != null) {
      throw new Exception("Tên tài khoản này đã được sử dụng bởi người khác trong hệ thống!");
    }

    String hashedPass = BCrypt.hashpw(passInCheck, BCrypt.gensalt(12));
    User newUser = new User(nameInCheck, hashedPass, mailInCheck, phoneInCheck);

    if (!userDAO.registerUser(newUser)) {
      throw new Exception(
          "Đã xảy ra lỗi hệ thống khi đồng bộ lưu trữ thông tin đăng ký vào cơ sở dữ liệu.");
    }

    logger.info("🎉 Đăng ký thành công tài khoản người dùng mới: " + nameInCheck);
    return newUser;
  }

  /** Kiểm tra thông tin tài khoản, trạng thái hoạt động và mật khẩu băm để phê duyệt đăng nhập. */
  public User login(LoginRequestDTO requestPayLoad) throws Exception {
    if (requestPayLoad == null) {
      throw new IllegalArgumentException("Yêu cầu thông tin đăng nhập không hợp lệ hoặc để trống!");
    }

    String nameInCheck = requestPayLoad.getUsername();
    String passInCheck = requestPayLoad.getPassword();

    if (nameInCheck == null || nameInCheck.trim().isEmpty()) {
      throw new Exception("Tên tài khoản đăng nhập không được bỏ trống.");
    }
    if (passInCheck == null || passInCheck.trim().isEmpty()) {
      throw new Exception("Mật khẩu đăng nhập không được bỏ trống.");
    }

    User userDB = userDAO.getUserByUsername(nameInCheck);
    if (userDB == null) {
      throw new Exception("Tên tài khoản hoặc mật khẩu bạn nhập không chính xác.");
    }

    if (UserStatus.BANNED.equals(userDB.getStatus())) {
      throw new Exception(
          "Tài khoản của bạn đã bị khóa (Ban) hoặc đình chỉ hoạt động bởi ban quản trị hệ thống.");
    }

    if (!BCrypt.checkpw(passInCheck, userDB.getPassword())) {
      throw new Exception("Tên tài khoản hoặc mật khẩu bạn nhập không chính xác.");
    }

    logger.info("🔑 Người dùng " + nameInCheck + " đăng nhập thành công vào hệ thống.");
    return userDB;
  }

  /** Trợ thủ kiểm tra định dạng dữ liệu (Email regex, Phone length) của luồng đăng ký. */
  private void validateRegisterFields(String username, String password, String email, String phone)
      throws Exception {
    if (username == null || username.trim().isEmpty()) {
      throw new Exception("Vui lòng nhập tên tài khoản (Username)!");
    } else if (password == null || password.trim().isEmpty()) {
      throw new Exception("Vui lòng điền mật khẩu đăng nhập!");
    } else if (password.length() < 6) {
      throw new Exception("Mật khẩu tài khoản phải có độ dài tối thiểu từ 6 ký tự trở lên!");
    } else if (email == null || email.trim().isEmpty()) {
      throw new Exception("Vui lòng điền thông tin địa chỉ Email!");
    } else if (phone == null || phone.trim().isEmpty()) {
      throw new Exception("Vui lòng nhập số điện thoại liên lạc!");
    }

    String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
    if (!email.matches(emailRegex)) {
      throw new Exception(
          "Định dạng thư điện tử không hợp lệ (Ví dụ chính xác: tennguoidung@gmail.com).");
    }

    String phoneRegex = "^0\\d{9}$";
    if (!phone.matches(phoneRegex)) {
      throw new Exception(
          "Số điện thoại không hợp lệ (Phải bao gồm chính xác 10 chữ số và bắt đầu bằng đầu số 0).");
    }
  }
}
