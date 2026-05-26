package org.example.server.services;

import org.example.core.dto.userDTO.LoginRequestDTO;
import org.example.core.dto.userDTO.RegisterRequestDTO;
import org.example.core.exception.AuthenticationException;
import org.example.core.exception.DataConflictException;
import org.example.core.exception.DatabaseAccessException;
import org.example.core.exception.InvalidUserDataException;
import org.example.core.exception.ResourceNotFoundException;
import org.example.core.exception.UserBannedException;
import org.example.core.models.users.User;
import org.example.core.shared.enums.UserStatus;
import org.example.server.daos.UserDAO;
import org.mindrot.jbcrypt.BCrypt;
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

  public User register(RegisterRequestDTO requestPayLoad) {
    if (requestPayLoad == null) {
      throw new InvalidUserDataException("Dữ liệu đăng ký không được gửi qua biểu mẫu trống!");
    }

    String nameInCheck = requestPayLoad.getUsername();
    String passInCheck = requestPayLoad.getPassword();
    String mailInCheck = requestPayLoad.getEmail();
    String phoneInCheck = requestPayLoad.getPhone();

    validateRegisterFields(nameInCheck, passInCheck, mailInCheck, phoneInCheck);

    try {
      User userDB = userDAO.getUserByUsername(nameInCheck);
      if (userDB != null) {
        throw new DataConflictException("Tên tài khoản này đã được sử dụng bởi người khác trong hệ thống!");
      }
    } catch (ResourceNotFoundException e) {
    }

    String hashedPass = BCrypt.hashpw(passInCheck, BCrypt.gensalt(12));
    User newUser = new User(nameInCheck, hashedPass, mailInCheck, phoneInCheck);

    boolean isSuccess = userDAO.registerUser(newUser);
    if (!isSuccess) {
      throw new DataConflictException("Đã xảy ra lỗi hệ thống khi đồng bộ lưu trữ thông tin đăng ký vào cơ sở dữ liệu.");
    }
    logger.info("Đăng ký thành công tài khoản người dùng mới: " + nameInCheck);
    return newUser;
  }

  public User login(LoginRequestDTO requestPayLoad) {
    if (requestPayLoad == null) {
      throw new InvalidUserDataException("Yêu cầu thông tin đăng nhập không hợp lệ hoặc để trống!");
    }

    String nameInCheck = requestPayLoad.getUsername();
    String passInCheck = requestPayLoad.getPassword();

    if (nameInCheck == null || nameInCheck.trim().isEmpty()) {
      throw new InvalidUserDataException("Tên tài khoản đăng nhập không được bỏ trống.");
    }
    if (passInCheck == null || passInCheck.trim().isEmpty()) {
      throw new InvalidUserDataException("Mật khẩu đăng nhập không được bỏ trống.");
    }

    User userDB;
    try {
      userDB = userDAO.getUserByUsername(nameInCheck);
      if (userDB == null) {
        throw new AuthenticationException("Tên tài khoản hoặc mật khẩu bạn nhập không chính xác.");
      }
    } catch (ResourceNotFoundException e) {
      throw new AuthenticationException("Tên tài khoản hoặc mật khẩu bạn nhập không chính xác.");
    }

    if (UserStatus.BANNED.equals(userDB.getStatus())) {
      throw new UserBannedException("Tài khoản của bạn đã bị khóa hoặc đình chỉ hoạt động bởi ban quản trị.");
    }

    if (!BCrypt.checkpw(passInCheck, userDB.getPassword())) {
      throw new AuthenticationException("Tên tài khoản hoặc mật khẩu bạn nhập không chính xác.");
    }

    logger.info("Người dùng " + nameInCheck + " đăng nhập thành công vào hệ thống.");
    return userDB;
  }

  private void validateRegisterFields(String username, String password, String email, String phone) {
    if (username == null || username.trim().isEmpty()) {
      throw new InvalidUserDataException("Vui lòng nhập tên tài khoản (Username)!");
    } else if (password == null || password.trim().isEmpty()) {
      throw new InvalidUserDataException("Vui lòng điền mật khẩu đăng nhập!");
    } else if (password.length() < 6) {
      throw new InvalidUserDataException("Mật khẩu tài khoản phải có độ dài tối thiểu từ 6 ký tự trở lên!");
    } else if (email == null || email.trim().isEmpty()) {
      throw new InvalidUserDataException("Vui lòng điền thông tin địa chỉ Email!");
    } else if (phone == null || phone.trim().isEmpty()) {
      throw new InvalidUserDataException("Vui lòng nhập số điện thoại liên lạc!");
    }

    String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
    if (!email.matches(emailRegex)) {
      throw new InvalidUserDataException("Định dạng thư điện tử không hợp lệ (Ví dụ chính xác: tennguoidung@gmail.com).");
    }

    String phoneRegex = "^0\\d{9}$";
    if (!phone.matches(phoneRegex)) {
      throw new InvalidUserDataException("Số điện thoại không hợp lệ (Phải bao gồm chính xác 10 chữ số và bắt đầu bằng số 0).");
    }
  }
}