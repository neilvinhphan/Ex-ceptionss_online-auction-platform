package org.example.server.daos;

import org.example.core.models.users.User;
import org.example.server.config.DBConnection;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
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

  public boolean registerUser(User user) {
    String sql = "INSERT INTO user (user_name, password, email, phone_number) VALUES (?,?,?,?)";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement preparedstatement = connection.prepareStatement(sql)) {
      preparedstatement.setString(1, user.getUserName());
      preparedstatement.setString(2, user.getPassword());
      preparedstatement.setString(3, user.getEmail());
      preparedstatement.setString(4, user.getPhone());
      return preparedstatement.executeUpdate() > 0;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean registerUserDB(User user) {
    if  (!isValidUser(user) || isUsernameExistsInDB(user.getUserName())) {
      return false;
    } else {
      String sql = "INSERT INTO user (user_name, password, email, phone_number) VALUES (?,?,?,?)";
      try (Connection connection = DBConnection.getConnection();
          PreparedStatement preparedstatement = connection.prepareStatement(sql)) {
        preparedstatement.setString(1, user.getUserName());
        preparedstatement.setString(2, user.getPassword());
        preparedstatement.setString(3, user.getEmail());
        preparedstatement.setString(4, user.getPhone());
        return preparedstatement.executeUpdate() > 0;
      } catch (SQLException |IOException e) {
        throw new RuntimeException(e);

    }
  }}

  public boolean isUsernameExistsInDB(String username) {
    String sql = "SELECT user_name FROM user WHERE user_name = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return true;
        }
      }
    } catch (SQLException |IOException e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  private User ResultSetUser(ResultSet rs) throws SQLException {
    User user = new User();
    user.setPassword(rs.getString("password"));
    user.setId(rs.getInt("user_id"));
    user.setUserName(rs.getString("user_name"));
    user.setBalance(rs.getBigDecimal("balance"));
    user.setEmail(rs.getString("email"));
    user.setPhone(rs.getString("phone_number"));
    user.setStatus(rs.getBoolean("status"));
    return user;
  }

  public User getUserByUsername(String username) {
    String sql = "SELECT * FROM user WHERE user_name = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return ResultSetUser(rs);
        }
      }
    } catch (SQLException |IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public int getUserIdInDB(String username) {
    String sql = "SELECT user_id FROM user WHERE user_name = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("user_id");
        }
      }
    } catch (SQLException |IOException e) {
      throw new RuntimeException(e);
    } return -1;
  }

  public User getUserByUserId(int id) {
    String sql = "SELECT * FROM user WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          User user = new User();
          user.setPassword(rs.getString("password"));
          user.setUserId(rs.getInt("user_id"));
          user.setUserName(rs.getString("user_name"));
          user.setBalance(rs.getBigDecimal("balance"));
          user.setEmail(rs.getString("email"));
          user.setPhone(rs.getString("phone_number"));
          user.setStatus(rs.getBoolean("status"));
          return user;
        }
      }
    } catch (SQLException| IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public boolean updateBalanceInDB(int id, BigDecimal balance) {
    String sql = "UPDATE user SET balance = ? WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setBigDecimal(1, balance);
      ps.setInt(2, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException |IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updatePasswordInDB(int id, String password) {
    String sql = "UPDATE user SET password = ? WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, password);
      ps.setInt(2, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException |IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updateRoleInDB(int id) {
    String sql = "UPDATE user SET role = 'seller' WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException |IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updatePhonenumberInDB(int id, String pn) {
    String sql = "UPDATE user SET phone_number = ? WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, pn);
      ps.setInt(2, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException |IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updateEmailInDB(int id, String email) {
    String sql = "UPDATE user SET email = ? WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, email);
      ps.setInt(2, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException |IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean banStatusInDB(int id) {
    String sql = "UPDATE user SET status = false WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException |IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean unbanStatusInDB(int id) {
    String sql = "UPDATE user SET status = true WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException |IOException e) {
      throw new RuntimeException(e);
    }
    }

  public boolean getStatusInDB(int id) {
    String sql = "SELECT status FROM user WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getBoolean("status");
        }
      }
    } catch (SQLException |IOException e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  public boolean updateRatingByUserId(int userId, double rating) {
    String sql = "UPDATE user SET rating = ? WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setDouble(1, rating);
      ps.setInt(2, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException |IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updateRatingByUsername(String username, double rating) throws Exception {
    String sql = "UPDATE user SET rating = ? WHERE user_name = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setDouble(1, rating);
      ps.setString(2, username);
      return ps.executeUpdate() > 0;
    }
  }

  public boolean isValidUser(User user) {
    if (user == null) return false;
    if (user.getUserName() == null || user.getUserName().isEmpty()) return false;
    if (user.getPassword() == null || user.getPassword().isEmpty()) return false;
    if (user.getEmail() == null || user.getEmail().isEmpty()) return false;
    if (user.getPhone() == null || user.getPhone().isEmpty()) return false;
    return true;
  }
}
