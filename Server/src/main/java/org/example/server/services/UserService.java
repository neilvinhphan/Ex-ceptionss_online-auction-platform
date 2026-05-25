package org.example.server.services;

import org.example.core.models.users.User;
import org.example.server.daos.UserDAO;
import org.mindrot.jbcrypt.BCrypt;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

/**
 * Dịch vụ xử lý hồ sơ tài khoản: Thực hiện nạp tiền kiểm tra bảo mật, nâng cấp phân quyền và quản
 * lý cấm đóng băng trạng thái thành viên.
 */
public class UserService {
  private static final Logger logger = Logger.getLogger(UserService.class.getName());
  private static volatile UserService instance = null;
  private final UserDAO userDAO;

  UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  /** Lấy instance duy nhất (Singleton) của UserService (Thread-safe). */
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

  // --- NHÓM PHƯƠNG THỨC THAY ĐỔI DỮ LIỆU (WRITE LOGIC) ---

  /**
   * Thực hiện quy trình nạp tiền vào ví điện tử: Xác minh mật khẩu và tiến hành cộng dồn số dư tài
   * khoản khả dụng.
   */
  public BigDecimal balanceDeposit(int userId, BigDecimal amount, String password)
      throws Exception {
    if (userId <= 0) {
      throw new Exception("Mã số tài khoản định danh người dùng không hợp lệ!");
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new Exception("Số tiền yêu cầu nạp vào tài khoản ví phải lớn hơn 0 VNĐ!");
    }
    if (password == null || password.trim().isEmpty()) {
      throw new Exception("Vui lòng nhập mật khẩu xác nhận danh tính để phê duyệt nạp tiền.");
    }

    User user = userDAO.getUserByUserId(userId);
    if (user == null) {
      throw new Exception("Tài khoản người dùng yêu cầu nạp tiền không tồn tại trên hệ thống.");
    }
    if (!BCrypt.checkpw(password, user.getPassword())) {
      throw new Exception("Mật khẩu tài khoản xác nhận không chính xác! Vui lòng kiểm tra lại.");
    }

    BigDecimal newBalance = user.getBalance().add(amount);
    if (!userDAO.updateBalanceInDB(userId, newBalance)) {
      throw new Exception(
          "Đã xảy ra lỗi hệ thống cục bộ khi cập nhật tăng số dư ví. Vui lòng thử lại sau ít phút!");
    }

    logger.info("💰 Tài khoản ID " + userId + " nạp ví thành công số tiền: " + amount + " VND.");
    return newBalance;
  }

  /** Thay đổi định danh quyền truy cập vai trò người dùng lên tư cách Người bán đồ (SELLER). */
  public boolean updateRole(int userId) throws Exception {
    if (userId <= 0) {
      throw new Exception("Mã người dùng yêu cầu nâng cấp quyền hạn vai trò không hợp lệ!");
    }
    User user = userDAO.getUserByUserId(userId);
    if (user == null) {
      throw new Exception("Tài khoản người dùng cần chuyển đổi vai trò không tồn tại.");
    }
    if (!userDAO.updateRoleInDB(userId)) {
      throw new Exception(
          "Lỗi hệ thống: Không thể lưu thay đổi vai trò quyền hạn mới vào Database.");
    }
    logger.info("👑 Đã nâng cấp thành công tài khoản ID " + userId + " sang vai trò SELLER.");
    return true;
  }

  /** Đóng băng cấm hoạt động (BANNED) tài khoản thành viên khỏi hệ thống. */
  public boolean banUser(int userId) throws Exception {
    if (userId <= 0) {
      throw new Exception("Mã tài khoản người dùng cần thực thi hình phạt khóa không hợp lệ.");
    }
    User user = userDAO.getUserByUserId(userId);
    if (user == null) {
      throw new Exception("Không tìm thấy thông tin tài khoản đối tượng cần cấm (Ban).");
    }
    if (!userDAO.banStatus(userId)) {
      throw new Exception(
          "Lỗi hệ thống: Thao tác khóa tài khoản bị hủy bỏ do trục trặc cơ sở dữ liệu.");
    }
    logger.warning("🚫 Ban quản trị đã thi hành hình phạt KHÓA tài khoản ID: " + userId);
    return true;
  }

  /**
   * Gỡ lệnh phạt và khôi phục trạng thái hoạt động bình thường (ACTIVE) cho tài khoản người dùng.
   */
  public boolean unbanUser(int userId) throws Exception {
    if (userId <= 0) {
      throw new Exception("Mã tài khoản người dùng cần gỡ bỏ lệnh cấm không hợp lệ.");
    }
    User user = userDAO.getUserByUserId(userId);
    if (user == null) {
      throw new Exception("Không tìm thấy dữ liệu tài khoản đối tượng cần mở khóa (Unban).");
    }
    if (!userDAO.unbanStatus(userId)) {
      throw new Exception(
          "Lỗi hệ thống: Thao tác mở khóa khôi phục tài khoản gặp lỗi xử lý dữ liệu.");
    }
    logger.info("🔓 Đã phục hồi và KÍCH HOẠT lại trạng thái cho tài khoản ID: " + userId);
    return true;
  }

  // --- NHÓM PHƯƠNG THỨC TRUY VẤN DỮ LIỆU (READ LOGIC) ---

  public User getUserById(int userId) throws Exception {
    if (userId <= 0) {
      throw new Exception("Mã định danh cá nhân tài khoản tra cứu không hợp lệ!");
    }
    User user = userDAO.getUserByUserId(userId);
    if (user == null) {
      throw new Exception(
          "Không tìm thấy bất kỳ hồ sơ tài khoản người dùng nào ứng với mã ID: " + userId);
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
}
