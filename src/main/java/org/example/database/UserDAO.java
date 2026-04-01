package org.example.database;

import org.example.backend.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    public boolean registerUser(User user) {
        String sql = "INSERT INTO user (user_name, password, email, phone_number) VALUES (?,?,?,?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedstatement = connection.prepareStatement(sql)) {
            preparedstatement.setString(1, user.getUserName());
            preparedstatement.setString(2, user.getPassword());
            preparedstatement.setString(3, user.getEmail());
            preparedstatement.setString(4, user.getPhone());
            return preparedstatement.execute();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public User getUserInformation(String username) throws SQLException {
        String sql = "SELECT * FROM user WHERE user_name = ?";
        try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) {
                    User user = new User();
                    user.setUserName(rs.getString("user_name"));
                    user.setBalance(rs.getBigDecimal("balance"));
                    return user;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    static void main() throws SQLException {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.getUserInformation("klbc_0211");
        if (user != null) {
      System.out.println(user.getUserName() + " " + user.getBalance());
        }
    }
}
