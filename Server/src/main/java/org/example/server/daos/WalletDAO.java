package org.example.server.daos;

import org.example.server.config.DBConnection;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WalletDAO {
    private static WalletDAO instance = null;
    private WalletDAO() {}
    public static WalletDAO getInstance() {
        if (instance == null) {
            synchronized (WalletDAO.class) {
                if (instance == null) {
                    instance = new WalletDAO();
                }
            }
        }
        return instance;
    }

    public BigDecimal getAvailableBalance(int userId) {
        String sql = """
                     SELECT (balance - (SELECT COALESCE(SUM(current_price), 0) 
                     FROM auction 
                     WHERE bidder_id = ? AND status = 'RUNNING')) AS available_balance FROM user WHERE user_id = ?""";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, userId);
            try(ResultSet rs = preparedStatement.executeQuery()) {
            if (rs.next()) {
                return rs.getBigDecimal("available_balance");
            } }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }
}
