package org.example.server.daos;

import org.example.core.models.entities.BidTransaction;
import org.example.server.config.DBConnection;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
        } return instance;
    }

    public boolean updateNewBid(int auctionId, int userId, BigDecimal currentprice) {
        String sql = "INSERT INTO bid (auction_id, user_id, bid_amount, bid_time) VALUES (?,?,?,?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, userId);
            ps.setBigDecimal(3, currentprice);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            int rowsUpdated = ps.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BigDecimal getCurrentPrice(int auctionId) {
        String sql =
                "SELECT COALESCE(("
                        + "SELECT MAX(bid_amount) FROM bid WHERE auction_id = ?"
                        + "), ("
                        + "SELECT start_price FROM auction_items WHERE id = ?"
                        + ")) AS current_price";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("current_price");
                }
            }
            return null;
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getBidIdByItemsId(int itemId) {
        return 0;
    }

    public List<BidTransaction> getBidTransactionByAuctionId(int auctionId) {
        List<BidTransaction> transactions = new ArrayList<>();
        String sql = "SELECT bid_id, auction_id, user_id, bid_amount, bid_time FROM bid WHERE auction_id = ? ORDER BY bid_time DESC";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BidTransaction transaction = new BidTransaction();
                    transaction.setId(rs.getInt("bid_id"));
                    transaction.setAucionId(rs.getInt("auction_id"));
                    transaction.setUserId(rs.getInt("user_id"));
                    transaction.setBidAmount(rs.getBigDecimal("bid_amount"));
                    Timestamp timestamp = rs.getTimestamp("bid_time");
                    if (timestamp != null) {
                        transaction.setBidTime(timestamp.toLocalDateTime());
                    }
                    transactions.add(transaction);
                }
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
        return transactions;
    }

    public List<BidTransaction> getBidTransactionByUserId(int userId) {
        List<BidTransaction> transactions = new ArrayList<>();
        String sql = "SELECT bid_id, auction_id, user_id, bid_amount, bid_time FROM bid WHERE user_id = ? ORDER BY bid_time DESC";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BidTransaction transaction = new BidTransaction();
                    transaction.setId(rs.getInt("bid_id"));
                    transaction.setAucionId(rs.getInt("auction_id"));
                    transaction.setUserId(rs.getInt("user_id"));
                    transaction.setBidAmount(rs.getBigDecimal("bid_amount"));
                    Timestamp timestamp = rs.getTimestamp("bid_time");
                    if (timestamp != null) {
                        transaction.setBidTime(timestamp.toLocalDateTime());
                    }
                    transactions.add(transaction);
                }
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
        return transactions;
    }
}
