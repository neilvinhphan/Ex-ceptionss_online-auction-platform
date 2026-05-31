package org.example.server.services;

import org.example.core.dto.userDTO.LoginRequestDTO;
import org.example.core.exception.AuthenticationException;
import org.example.core.exception.DatabaseAccessException;
import org.example.core.exception.InvalidUserDataException;
import org.example.core.exception.ResourceNotFoundException;
import org.example.core.models.users.User;
import org.example.server.daos.UserDAO;
import org.mindrot.jbcrypt.BCrypt;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

/**
 * Dịch vụ xử lý hồ sơ tài khoản người dùng.
 */
public class UserService {
  private static final Logger logger = Logger.getLogger(UserService.class.getName());
  private static volatile UserService instance = null;
  private final UserDAO userDAO;

  UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  public static UserService getInstance() {
    if (instance == null) {
      synchronized (UserService.class) {
        if (instance == null) {
          instance = new UserService(UserDAO.getInstance());
        }
      }
    }
    return instance;
  }

  public BigDecimal balanceDeposit(int userId, BigDecimal amount, String password) {
    if (userId <= 0) {
      throw new InvalidUserDataException("Mã số tài khoản định danh người dùng không hợp lệ!");
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidUserDataException("Số tiền yêu cầu nạp vào tài khoản ví phải lớn hơn 0 VNĐ!");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new InvalidUserDataException("Vui lòng nhập mật khẩu xác nhận danh tính để phê duyệt nạp tiền.");
    }

    User user = userDAO.getUserByUserId(userId);
    if (!BCrypt.checkpw(password, user.getPassword())) {
      throw new AuthenticationException("Mật khẩu tài khoản xác nhận không chính xác! Vui lòng kiểm tra lại.");
    }

    BigDecimal newBalance = user.getBalance().add(amount);
    boolean isSuccess = userDAO.updateBalanceInDB(userId, newBalance);
    if(!isSuccess) {
      throw new DatabaseAccessException("Đã xảy ra lỗi trong quá trình cập nhật số dư tài khoản! Vui lòng thử lại sau.");
    }

    logger.info("💰 Tài khoản ID " + userId + " nạp ví thành công số tiền: " + amount + " VND.");
    return newBalance;
  }

  public boolean updateRole(int userId) {
    if (userId <= 0) {
      throw new InvalidUserDataException("Mã người dùng yêu cầu nâng cấp quyền hạn vai trò không hợp lệ!");
    }
    userDAO.getUserByUserId(userId);
    return userDAO.updateRoleInDB(userId);
  }

  public boolean banUser(int userId) {
    if (userId <= 0) {
      throw new InvalidUserDataException("Mã tài khoản người dùng cần thực thi hình phạt khóa không hợp lệ.");
    }
    userDAO.getUserByUserId(userId);
    return userDAO.banStatus(userId);
  }

  public boolean unbanUser(int userId) {
    if (userId <= 0) {
      throw new InvalidUserDataException("Mã tài khoản người dùng cần gỡ bỏ lệnh cấm không hợp lệ.");
    }
    userDAO.getUserByUserId(userId);
    return userDAO.unbanStatus(userId);
  }

  public User getUserById(int userId) {
    if (userId <= 0) {
      throw new InvalidUserDataException("Mã định danh cá nhân tài khoản tra cứu không hợp lệ!");
    }
    return userDAO.getUserByUserId(userId);
  }

  public List<User> getAllUsers() {
    return userDAO.getAllUsers();
  }
}