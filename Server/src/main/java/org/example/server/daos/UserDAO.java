package org.example.server.daos;

import org.example.core.models.users.User;
import org.example.core.shared.enums.RoleType;
import org.example.core.shared.enums.UserStatus;
import org.example.core.exception.DatabaseAccessException;
import org.example.core.exception.ResourceNotFoundException;
import org.example.core.exception.DataConflictException;
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

/** Lớp truy cập dữ liệu (DAO) chịu trách nhiệm quản lý tài khoản, phân quyền, số dư ví. */
public class UserDAO {
  private static final Logger logger = Logger.getLogger(UserDAO.class.getName());
  private static volatile UserDAO instance = null;

  private UserDAO() {}

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
      if (e.getErrorCode() == 1062) {
        throw new DataConflictException("Tên đăng nhập (Username) hoặc Email này đã tồn tại trên hệ thống!");
      }
      throw new DatabaseAccessException("Đăng ký thông tin tài khoản thất bại do lỗi hệ thống dữ liệu.", e);
    }
  }

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
      throw new DatabaseAccessException("Cập nhật số dư tài khoản vào cơ sở dữ liệu thất bại.", e);
    }
  }

  public boolean updateRoleInDB(int userId) {
    String sql = "UPDATE user SET role = 'SELLER' WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi nâng cấp quyền thành SELLER cho User ID: " + userId, e);
      throw new DatabaseAccessException("Nâng cấp phân quyền tài khoản người bán thất bại.", e);
    }
  }

  public boolean banStatus(int userId) {
    String sql = "UPDATE user SET status = 'BANNED' WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi thực thi cấm tài khoản (BAN) cho User ID: " + userId, e);
      throw new DatabaseAccessException("Khóa trạng thái hoạt động tài khoản người dùng thất bại.", e);
    }
  }

  public boolean unbanStatus(int userId) {
    String sql = "UPDATE user SET status = 'ACTIVE' WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi thực thi gỡ cấm (UNBAN) cho User ID: " + userId, e);
      throw new DatabaseAccessException("Mở khóa kích hoạt lại trạng thái tài khoản thất bại.", e);
    }
  }

  // --- NHÓM PHƯƠNG THỨC TRUY VẤN DỮ LIỆU (READ) ---

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
      throw new DatabaseAccessException("Truy vấn tìm kiếm thông tin người dùng bằng tài khoản thất bại.", e);
    }
    throw new ResourceNotFoundException("Tên tài khoản hoặc mật khẩu bạn nhập không chính xác.");
  }

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
      throw new DatabaseAccessException("Truy vấn tìm kiếm thông tin người dùng bằng mã ID thất bại.", e);
    }
    throw new ResourceNotFoundException("Không tồn tại người dùng nào ứng với mã ID: " + userId);
  }

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
      throw new DatabaseAccessException("Truy vấn lấy chuỗi tên tài khoản thất bại.", e);
    }
    throw new ResourceNotFoundException("Không tìm thấy tên hiển thị của người dùng có ID: " + userId);
  }

  public List<User> getAllUsers() {
    List<User> users = new ArrayList<>();
    String sql = "SELECT * FROM user WHERE role IN ('SELLER', 'BIDDER')";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        users.add(mapResultSetToUser(rs));
      }
      return users;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi hệ thống khi tải toàn bộ danh sách User", e);
      throw new DatabaseAccessException("Không thể trích xuất danh sách toàn bộ người dùng hệ thống.", e);
    }
  }
}