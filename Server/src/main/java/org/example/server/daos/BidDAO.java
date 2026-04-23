package org.example.server.daos;

import org.example.core.models.entities.BidTransaction;
import org.example.server.config.DBConnection;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
        } return instance;
    }

    public boolean updateNewBid(int auctionId, int userId, BigDecimal currentprice) {
        String sql = "INSERT INTO bid (auction_id, )";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBigDecimal(1, currentprice);
            ps.setInt(2, userId);
            ps.setInt(3, auctionId);
            int rowsUpdated = ps.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BigDecimal getCurrentPrice(int auctionId) {
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
        return 0;
    }

    public List<BidTransaction> getBidTransactionByAuctionId(int auctionId) {
        List<BidTransaction> transactions = new ArrayList<>();
        return transactions;
    }

    public List<BidTransaction> getBidTransactionByUserId(int userId) {
        List<BidTransaction> transactions = new ArrayList<>();
        return transactions;
    }
}
