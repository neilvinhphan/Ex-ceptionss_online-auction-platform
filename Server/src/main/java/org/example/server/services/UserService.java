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
      throw new Exception("Mã số tài khoản định danh người dùng không hợp lệ!");
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new Exception("Số tiền yêu cầu nạp vào tài khoản ví phải lớn hơn 0 VNĐ!");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new Exception("Vui lòng nhập mật khẩu xác nhận danh tính để phê duyệt nạp tiền.");
    }

    // 2. Sau khi dữ liệu đầu vào đã sạch mới bắt đầu gọi DAO
    User user = userDAO.getUserByUserId(userId);
    if (user == null) {
      throw new Exception("Tài khoản người dùng yêu cầu nạp tiền không tồn tại trên hệ thống.");
    }
    if (!BCrypt.checkpw(password, user.getPassword())) {
      throw new Exception("Mật khẩu tài khoản xác nhận không chính xác! Vui lòng kiểm tra lại.");
    }

    BigDecimal currentBalance = user.getBalance();
    BigDecimal newBalance = currentBalance.add(amount);

    boolean isSuccess = userDAO.updateBalanceInDB(userId, newBalance);
    if (!isSuccess) {
      throw new Exception("Đã xảy ra lỗi hệ thống cục bộ khi cập nhật tăng số dư ví. Vui lòng thử lại sau ít phút!");
    }
    return newBalance;
  }

  public boolean updateRole(int userId) throws Exception {
    if (userId <= 0) {
      throw new Exception("Mã người dùng yêu cầu nâng cấp quyền hạn vai trò không hợp lệ!");
    }
    User user = userDAO.getUserByUserId(userId);
    if (user == null) {
      throw new Exception("Tài khoản người dùng cần chuyển đổi vai trò không tồn tại.");
    }
    boolean success = userDAO.updateRoleInDB(userId);
    if (!success) {
      throw new Exception("Lỗi hệ thống: Không thể lưu thay đổi vai trò quyền hạn mới vào Database.");
    }
    return true;
  }

  public User getUserById(int userId) throws Exception {
    if (userId <= 0) {
      throw new Exception("Mã định danh cá nhân tài khoản tra cứu không hợp lệ!");
    }
    User user = userDAO.getUserByUserId(userId);
    if (user == null) {
      throw new Exception("Không tìm thấy bất kỳ hồ sơ tài khoản người dùng nào ứng với mã ID: " + userId);
    }
    return user;
  }

  public List<User> getAllUsers() throws Exception {
    List<User> list = userDAO.getAllUsers();
    if (list == null) {
      throw new Exception("Lỗi hệ thống: Không thể truy xuất danh sách toàn bộ người dùng.");
    }
    return list;
  }

  public boolean banUser(int userId) throws Exception {
    if (userId <= 0) {
      throw new Exception("Mã tài khoản người dùng cần thực thi hình phạt khóa không hợp lệ.");
    }
    User user = userDAO.getUserByUserId(userId);
    if (user == null) {
      throw new Exception("Không tìm thấy thông tin tài khoản đối tượng cần cấm (Ban).");
    }
    boolean success = userDAO.banStatus(userId);
    if (!success) {
      throw new Exception("Lỗi hệ thống: Thao tác khóa tài khoản bị hủy bỏ do trục trặc cơ sở dữ liệu.");
    }
    return true;
  }

  public boolean unbanUser(int userId) throws Exception {
    if (userId <= 0) {
      throw new Exception("Mã tài khoản người dùng cần gỡ bỏ lệnh cấm không hợp lệ.");
    }
    User user = userDAO.getUserByUserId(userId);
    if (user == null) {
      throw new Exception("Không tìm thấy dữ liệu tài khoản đối tượng cần mở khóa (Unban).");
    }
    boolean success = userDAO.unbanStatus(userId);
    if (!success) {
      throw new Exception("Lỗi hệ thống: Thao tác mở khóa khôi phục tài khoản gặp lỗi xử lý dữ liệu.");
    }
    return true;
  }
}