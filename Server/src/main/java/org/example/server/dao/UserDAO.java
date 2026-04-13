package org.example.server.dao;

import org.example.core.models.users.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

  public boolean registerUser(User user) throws Exception {
    String sql = "INSERT INTO user (user_name, password, email, phone_number) VALUES (?,?,?,?)";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement preparedstatement = connection.prepareStatement(sql)) {
      preparedstatement.setString(1, user.getUserName());
      preparedstatement.setString(2, user.getPassword());
      preparedstatement.setString(3, user.getEmail());
      preparedstatement.setString(4, user.getPhone());
      return preparedstatement.executeUpdate() > 0;
    }
  }

  public User getUserByUsername(String username) throws Exception {
    String sql = "SELECT * FROM user WHERE user_name = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
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
      }
    }
    return null;
  }

  public void updateBalance(String username, BigDecimal amount, String act) throws Exception {
    String sql = "UPDATE user SET balance = balance + ? WHERE user_name = ?";
    BigDecimal finalAmount;
    if (act.equals("Deposit")) {
      finalAmount = amount;
    } else {
      finalAmount = amount.negate();
    }
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setBigDecimal(1, finalAmount);
      ps.setString(2, username);
      ps.executeUpdate();
    }
  }
}
