package org.example.server.daos;

import org.example.core.shared.enums.WalletTransactionType;
import org.example.core.exception.DatabaseAccessException;
import org.example.core.exception.ResourceNotFoundException;
import org.example.server.config.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Lớp truy cập dữ liệu (DAO) quản lý tính toán dòng tiền khả dụng và ghi lịch sử ví. */
public class WalletDAO {
  private static final Logger logger = Logger.getLogger(WalletDAO.class.getName());
  private static volatile WalletDAO instance = null;

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

  // --- NHÓM PHƯƠNG THỨC GHI DỮ LIỆU (WRITE) ---

  public void insertWalletTransaction(int userId, BigDecimal amount, WalletTransactionType type, int auctionId) {
    String sql = "INSERT INTO wallet_transaction (user_id, amount, transaction_type, reference_id) VALUES (?,?,?,?)";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      ps.setBigDecimal(2, amount);
      ps.setString(3, String.valueOf(type));
      ps.setInt(4, auctionId);
      ps.executeUpdate();
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi chèn lịch sử giao dịch ví cho User ID: " + userId + ", Loại: " + type, e);
      throw new DatabaseAccessException("Ghi nhận nhật ký lịch sử biến động số dư ví thất bại.", e);
    }
  }

  // --- NHÓM PHƯƠNG THỨC TRUY VẤN DỮ LIỆU (READ) ---

  public BigDecimal getAvailableBalance(int userId) {
    String sql = """
      SELECT (balance - (SELECT COALESCE(SUM(highest_price), 0)
      FROM auction
      WHERE bidder_id = ? AND status IN ('RUNNING', 'FINISHED'))) AS available_balance 
      FROM `user` 
      WHERE user_id = ?""";

    try (Connection connection = DBConnection.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      preparedStatement.setInt(1, userId);
      preparedStatement.setInt(2, userId);

      try (ResultSet rs = preparedStatement.executeQuery()) {
        if (rs.next()) {
          BigDecimal balance = rs.getBigDecimal("available_balance");
          return balance != null ? balance : BigDecimal.ZERO;
        }
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi tính toán số dư đóng băng khả dụng cho User ID: " + userId, e);
      throw new DatabaseAccessException("Tính toán hạn mức số dư ví khả dụng hiện tại thất bại.", e);
    }
    throw new ResourceNotFoundException("Không tìm thấy tài khoản người dùng ứng với mã ID để tính số dư: " + userId);
  }
}