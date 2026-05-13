package org.example.server.daos;

import org.example.core.models.users.User;
import org.example.core.shared.enums.RoleType;
import org.example.core.shared.enums.UserStatus;
import org.example.server.config.DBConnection;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
  private static UserDAO instance = null;

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

  public boolean registerUser(User user) {
    String sql = "INSERT INTO user (user_name, password, email, phone_number) VALUES (?,?,?,?)";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement preparedstatement = connection.prepareStatement(sql)) {
      preparedstatement.setString(1, user.getUserName());
      preparedstatement.setString(2, user.getPassword());
      preparedstatement.setString(3, user.getEmail());
      preparedstatement.setString(4, user.getPhone());
      return preparedstatement.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

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
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
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
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public String getUserNameByUserId(int userId) {
    String sql = "SELECT user_name FROM user WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("user_name");
  }}     } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public boolean updateBalanceInDB(int userId, BigDecimal balance) {
    if (balance.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException();
    }
    String sql = "UPDATE user SET balance = ? WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setBigDecimal(1, balance);
      ps.setInt(2, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updatePasswordInDB(int userId, String password) {
    String sql = "UPDATE user SET password = ? WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, password);
      ps.setInt(2, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updateRoleInDB(int userId) {
    String sql = "UPDATE user SET role = 'SELLER' WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updatePhonenumberInDB(int userId, String pn) {
    String sql = "UPDATE user SET phone_number = ? WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, pn);
      ps.setInt(2, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updateEmailInDB(int userId, String email) {
    String sql = "UPDATE user SET email = ? WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, email);
      ps.setInt(2, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean banStatus(int userId) {
    String sql = "UPDATE user SET status = 'BANNED' WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean unbanStatus(int userId) {
    String sql = "UPDATE user SET status = 'ACTIVE' WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public UserStatus getUserStatusInDB(int userId) {
    String sql = "SELECT status FROM user WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          String statusString = rs.getString("status");
          return UserStatus.valueOf(statusString);
        } else {
          return null;
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updateRatingByUsername(String username, double rating) {
    String sql = "UPDATE user SET rating = ? WHERE user_name = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setDouble(1, rating);
      ps.setString(2, username);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<User> getAllUsers() {
    List<User> users = new ArrayList<>();
    String sql = "SELECT * FROM user";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

      while (rs.next()) {
        User u = mapResultSetToUser(rs);
        users.add(u);
      }

    } catch (SQLException | IOException e) {
      e.printStackTrace();
    }

    return users;
  }
}
