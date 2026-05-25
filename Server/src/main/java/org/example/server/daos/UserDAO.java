package org.example.server.daos;

import org.example.core.models.users.User;
import org.example.core.shared.enums.RoleType;
import org.example.core.shared.enums.UserStatus;
import org.example.server.config.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lớp truy cập dữ liệu (DAO) chịu trách nhiệm quản lý tài khoản, phân quyền, số dư ví và trạng thái của người dùng.
 */
public class UserDAO {
  private static final Logger logger = Logger.getLogger(UserDAO.class.getName());
  private static volatile UserDAO instance = null;

  private UserDAO() {}

  /**
   * Lấy instance duy nhất (Singleton) của UserDAO (Thread-safe).
   */
  public static UserDAO getInstance() {
    if (instance == null) {
      synchronized (UserDAO.class) {
        if (instance == null) {
          instance = new UserDAO();
        }
      }
    }
    return instance;
  }

  // --- TRỢ THỦ ĐÓNG GÓI NỘI BỘ (HELPER) ---
  private User mapResultSetToUser(ResultSet rs) throws SQLException {
    User user = new User();
    user.setUserId(rs.getInt("user_id"));
    user.setUserName(rs.getString("user_name"));
    user.setPassword(rs.getString("password"));
    user.setEmail(rs.getString("email"));
    user.setPhone(rs.getString("phone_number"));
    user.setBalance(rs.getBigDecimal("balance"));
    user.setStatus(UserStatus.valueOf(rs.getString("status")));
    user.setRole(RoleType.valueOf(rs.getString("role")));
    return user;
  }

  // --- NHÓM PHƯƠNG THỨC GHI DỮ LIỆU (WRITE) ---

  /**
   * Đăng ký tạo thông tin tài khoản người dùng mới vào hệ thống.
   */
  public boolean registerUser(User user) {
    String sql = "INSERT INTO user (user_name, password, email, phone_number) VALUES (?,?,?,?)";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement preparedstatement = connection.prepareStatement(sql)) {
      preparedstatement.setString(1, user.getUserName());
      preparedstatement.setString(2, user.getPassword());
      preparedstatement.setString(3, user.getEmail());
      preparedstatement.setString(4, user.getPhone());
      return preparedstatement.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi đăng ký tài khoản mới cho Username: " + user.getUserName(), e);
      throw new RuntimeException("Đăng ký tài khoản người dùng thất bại", e);
    }
  }

  /**
   * Cập nhật số dư ví tiền tài khoản người dùng trực tiếp trong Database.
   */
  public boolean updateBalanceInDB(int userId, BigDecimal balance) {
    if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Số dư cập nhật không hợp lệ hoặc bị âm!");
    }
    String sql = "UPDATE user SET balance = ? WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setBigDecimal(1, balance);
      ps.setInt(2, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi cập nhật số dư ví cho User ID: " + userId, e);
      throw new RuntimeException("Cập nhật số dư tài khoản thất bại", e);
    }
  }

  /**
   * Nâng cấp quyền tài khoản người dùng lên vai trò SELLER khi họ đăng bán đồ.
   */
  public boolean updateRoleInDB(int userId) {
    String sql = "UPDATE user SET role = 'SELLER' WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi nâng cấp quyền thành SELLER cho User ID: " + userId, e);
      throw new RuntimeException("Nâng cấp quyền tài khoản người bán thất bại", e);
    }
  }

  /**
   * Khóa tài khoản người dùng (BANNED) khi vi phạm quy chế đấu giá.
   */
  public boolean banStatus(int userId) {
    String sql = "UPDATE user SET status = 'BANNED' WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi thực thi cấm tài khoản (BAN) cho User ID: " + userId, e);
      throw new RuntimeException("Khóa tài khoản người dùng thất bại", e);
    }
  }

  /**
   * Mở khóa kích hoạt lại tài khoản người dùng về trạng thái ACTIVE bình thường.
   */
  public boolean unbanStatus(int userId) {
    String sql = "UPDATE user SET status = 'ACTIVE' WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi thực thi gỡ cấm (UNBAN) cho User ID: " + userId, e);
      throw new RuntimeException("Kích hoạt lại tài khoản thất bại", e);
    }
  }

  // --- NHÓM PHƯƠNG THỨC TRUY VẤN DỮ LIỆU (READ) ---

  /**
   * Tìm kiếm thực thể tài khoản User dựa vào chuỗi định danh tên đăng nhập unique.
   */
  public User getUserByUsername(String username) {
    String sql = "SELECT * FROM user WHERE user_name = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapResultSetToUser(rs);
        }
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi tìm kiếm tài khoản theo username: " + username, e);
      throw new RuntimeException("Truy vấn người dùng bằng tên tài khoản thất bại", e);
    }
    return null;
  }

  /**
   * Tìm kiếm thực thể tài khoản User bằng ID khóa chính hệ thống.
   */
  public User getUserByUserId(int userId) {
    String sql = "SELECT * FROM user WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapResultSetToUser(rs);
        }
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi tìm kiếm tài khoản theo ID: " + userId, e);
      throw new RuntimeException("Truy vấn người dùng bằng mã ID thất bại", e);
    }
    return null;
  }

  /**
   * Lấy nhanh tên hiển thị (Username) của một tài khoản thông qua ID.
   */
  public String getUserNameByUserId(int userId) {
    String sql = "SELECT user_name FROM user WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("user_name");
        }
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi truy vấn lấy chuỗi tên của User ID: " + userId, e);
      throw new RuntimeException("Lấy tên tài khoản thất bại", e);
    }
    return null;
  }

  /**
   * Kéo toàn bộ danh sách khách hàng (Chỉ bao gồm vai trò SELLER và BIDDER) phục vụ module quản lý User của Admin.
   */
  public List<User> getAllUsers() {
    List<User> users = new ArrayList<>();
    String sql = "SELECT * FROM user WHERE role IN ('SELLER', 'BIDDER')";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        users.add(mapResultSetToUser(rs));
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi hệ thống khi tải toàn bộ danh sách User", e);
      throw new RuntimeException("Không thể kết xuất danh sách toàn bộ người dùng", e);
    }
    return users;
  }
}