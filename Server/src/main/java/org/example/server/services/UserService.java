package org.example.server.services;

import org.example.core.models.users.SellerProfile;
import org.example.core.models.users.User;
import org.example.server.daos.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.util.List;

public class UserService {
  private final UserDAO userDAO;
  private static volatile UserService instance = null;

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

  public BigDecimal balanceDeposit(int userId, BigDecimal amount, String password) throws Exception {
    // 1. Đưa các chốt chặn chắt lọc dữ liệu lên đầu tiên
    if (userId <= 0) {
      throw new Exception("ID người dùng không hợp lệ");
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new Exception("Số tiền nạp phải lớn hơn 0");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new Exception("Vui lòng nhập mật khẩu xác nhận.");
    }

    // 2. Sau khi dữ liệu đầu vào đã sạch mới bắt đầu gọi DAO
    User user = userDAO.getUserByUserId(userId);
    if (user == null) {
      throw new Exception("Người dùng không tồn tại.");
    }
    if (!BCrypt.checkpw(password, user.getPassword())) {
      throw new Exception("Mật khẩu xác nhận không chính xác!");
    }

    BigDecimal currentBalance = user.getBalance();
    BigDecimal newBalance = currentBalance.add(amount);

    boolean isSuccess = userDAO.updateBalanceInDB(userId, newBalance);
    if (!isSuccess) {
      throw new Exception("Đã xảy ra lỗi hệ thống khi cập nhật số dư. Vui lòng thử lại!");
    }
    return newBalance;
  }

  public boolean updateRole(int userId) {
    return userDAO.updateRoleInDB(userId);
  }

  public User getUserById(int userId) {
    return userDAO.getUserByUserId(userId);
  }

  public List<User> getAllUsers() {
    return userDAO.getAllUsers();
  }

  public boolean banUser(int userId) {
    return userDAO.banStatus(userId);
  }

  public boolean unbanUser(int userId) {
    return userDAO.unbanStatus(userId);
  }
}
