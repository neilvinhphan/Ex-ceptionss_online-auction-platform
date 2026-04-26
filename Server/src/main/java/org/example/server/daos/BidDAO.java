package org.example.server.daos;

import org.example.core.models.entities.BidTransaction;
import org.example.server.config.DBConnection;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {
  private static volatile BidDAO instance;

  private BidDAO() {}

  public static BidDAO getInstance() {
    if (instance == null) {
      synchronized (BidDAO.class) {
        if (instance == null) {
          instance = new BidDAO();
        }
      }
    }
    return instance;
  }

  public int updateNewBid(int auctionId, int userId, BigDecimal currentPrice) {
    String sql = "INSERT INTO bid (auction_id, bidder_id, current_price) VALUES (?, ?, ?)";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      ps.setInt(2, userId);
      ps.setBigDecimal(3, currentPrice);
      try(ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    } return -1;
  }

  public BigDecimal getCurrentPriceByAuctionId(int auctionId) {
    String sql = "SELECT current_price FROM auctions WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      return ps.executeQuery().getBigDecimal("current_price");
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public int getBidIdByItemsId(int itemId) {
    String sql = "SELECT bid_id FROM bid WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("bid_id");
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return -1;
  }

  public int getBidIdByAuctionId(int auctionId) {
    String sql = "SELECT bid_id FROM bid WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("bid_id");
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return -1;
  }

  public BigDecimal getHighestPriceByItemId(int itemId) {
    String sql = "SELECT MAX(current_price) AS bid_amount FROM bid WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getBigDecimal("current_price");
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return BigDecimal.ZERO;
  }

  public BigDecimal getHighestPriceByAuctionId(int auctionId) {
    String sql = "SELECT Max(current_price) AS bid_amount FROM bid WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getBigDecimal("bid_amount");
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return BigDecimal.ZERO;
  }

  public List<BidTransaction> getBidTransactionByAuctionId(int auctionId) {
    List<BidTransaction> transactions = new ArrayList<>();
    String sql = "SELECT * FROM bid WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          BidTransaction transaction = new BidTransaction();
          transaction.setBidId(rs.getInt("bid_id"));
          transaction.setAuctionId(rs.getInt("auction_id"));
          transaction.setBidderId(rs.getInt("user_id"));
          transaction.setBidAmount(rs.getBigDecimal("bid_amount"));
          transaction.setBidTime(rs.getTimestamp("bid_time").toLocalDateTime());
          transactions.add(transaction);
        }
      }
      return transactions;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<BidTransaction> getBidTransactionByUserId(int userId) {
    List<BidTransaction> transactions = new ArrayList<>();
    String sql = "SELECT * FROM bid WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          BidTransaction bidTransaction = new BidTransaction();
          bidTransaction.setBidId(rs.getInt("bid_id"));
          bidTransaction.setAuctionId(rs.getInt("auction_id"));
          bidTransaction.setBidderId(rs.getInt("user_id"));
          bidTransaction.setBidAmount(rs.getBigDecimal("bid_amount"));
          bidTransaction.setBidTime(rs.getTimestamp("bid_time").toLocalDateTime());
          transactions.add(bidTransaction);
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return transactions;
  }

  public boolean removeBidTransactionByBidderId(int bidderId) {
    String sql = "DELETE FROM bid WHERE user_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, bidderId);
      int rowsDeleted = ps.executeUpdate();
      return rowsDeleted > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean removeBidTransactionByAuctionId(int auctionId) {
    String sql = "DELETE FROM bid WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      int rowsDeleted = ps.executeUpdate();
      return rowsDeleted > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
