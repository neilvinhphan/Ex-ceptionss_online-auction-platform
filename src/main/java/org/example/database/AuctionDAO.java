package org.example.database;

import org.example.backend.models.Auction;
import org.example.backend.models.BidTransaction;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    /**
     * Lấy danh sách tất cả phiên đấu giá đang diễn ra (status = 'ACTIVE').
     * Mỗi phiên đã bao gồm giá hiện tại và người dẫn đầu.
     */
    public List<Auction> getActiveAuctions() {
        List<Auction> auctions = new ArrayList<>();
        String sql = """
                SELECT a.id, a.item_name, a.item_description, a.status,
                       a.start_time, a.end_time, a.starting_price,
                       COALESCE(MAX(b.bid_amount), a.starting_price) AS current_price,
                       (SELECT b2.bidder_username
                        FROM bid_transaction b2
                        WHERE b2.auction_id = a.id
                        ORDER BY b2.bid_amount DESC, b2.bid_time DESC
                        LIMIT 1) AS leader_username
                FROM auction a
                LEFT JOIN bid_transaction b ON b.auction_id = a.id
                WHERE a.status = 'ACTIVE'
                GROUP BY a.id, a.item_name, a.item_description, a.status,
                         a.start_time, a.end_time, a.starting_price
                ORDER BY a.id
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Auction auction = mapRow(rs);
                auctions.add(auction);
            }
        } catch (Exception e) {
            System.err.println("Failed to retrieve active auctions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Làm mới thông tin giá hiện tại và người dẫn đầu cho một phiên đấu giá.
     */
    public Auction refreshAuction(int auctionId) {
        String sql = """
                SELECT a.id, a.item_name, a.item_description, a.status,
                       a.start_time, a.end_time, a.starting_price,
                       COALESCE(MAX(b.bid_amount), a.starting_price) AS current_price,
                       (SELECT b2.bidder_username
                        FROM bid_transaction b2
                        WHERE b2.auction_id = a.id
                        ORDER BY b2.bid_amount DESC, b2.bid_time DESC
                        LIMIT 1) AS leader_username
                FROM auction a
                LEFT JOIN bid_transaction b ON b.auction_id = a.id
                WHERE a.id = ?
                GROUP BY a.id, a.item_name, a.item_description, a.status,
                         a.start_time, a.end_time, a.starting_price
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to refresh auction with ID: " + auctionId + " - " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Đặt giá cho một phiên đấu giá.
     * Trả về thông báo kết quả: null nếu thành công, chuỗi lỗi nếu thất bại.
     */
    public String placeBid(int auctionId, String bidderUsername, BigDecimal bidAmount) {
        // Kiểm tra giá hiện tại
        String checkSql = """
                SELECT COALESCE(MAX(b.bid_amount), a.starting_price) AS current_price, a.status
                FROM auction a
                LEFT JOIN bid_transaction b ON b.auction_id = a.id
                WHERE a.id = ?
                GROUP BY a.id, a.starting_price, a.status
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setInt(1, auctionId);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (!rs.next()) {
                    return "Không tìm thấy phiên đấu giá.";
                }
                String status = rs.getString("status");
                if (!"ACTIVE".equals(status)) {
                    return "Phiên đấu giá đã kết thúc.";
                }
                BigDecimal currentPrice = rs.getBigDecimal("current_price");
                if (bidAmount.compareTo(currentPrice) <= 0) {
                    return "Giá đặt phải cao hơn giá hiện tại: "
                            + String.format("%,.0f VNĐ", currentPrice);
                }
            }

            // Chèn giao dịch đặt giá
            String insertSql = "INSERT INTO bid_transaction (auction_id, bidder_username, bid_amount, bid_time) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                insertPs.setInt(1, auctionId);
                insertPs.setString(2, bidderUsername);
                insertPs.setBigDecimal(3, bidAmount);
                insertPs.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                insertPs.executeUpdate();
            }
            return null; // thành công
        } catch (Exception e) {
            System.err.println("Failed to place bid for auction " + auctionId + " by user " + bidderUsername + ": " + e.getMessage());
            e.printStackTrace();
            return "Lỗi kết nối: " + e.getMessage();
        }
    }

    /**
     * Lấy lịch sử đặt giá của một phiên đấu giá.
     */
    public List<BidTransaction> getBidHistory(int auctionId) {
        List<BidTransaction> history = new ArrayList<>();
        String sql = "SELECT id, auction_id, bidder_username, bid_amount, bid_time FROM bid_transaction WHERE auction_id = ? ORDER BY bid_amount DESC, bid_time DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BidTransaction bt = new BidTransaction(
                            rs.getInt("id"),
                            rs.getInt("auction_id"),
                            rs.getString("bidder_username"),
                            rs.getBigDecimal("bid_amount"),
                            rs.getTimestamp("bid_time").toLocalDateTime()
                    );
                    history.add(bt);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to retrieve bid history for auction with ID: " + auctionId + " - " + e.getMessage());
            e.printStackTrace();
        }
        return history;
    }

    private Auction mapRow(ResultSet rs) throws SQLException {
        Timestamp startTs = rs.getTimestamp("start_time");
        Timestamp endTs = rs.getTimestamp("end_time");
        return new Auction(
                rs.getInt("id"),
                rs.getString("item_name"),
                rs.getString("item_description"),
                rs.getString("status"),
                startTs != null ? startTs.toLocalDateTime() : null,
                endTs != null ? endTs.toLocalDateTime() : null,
                rs.getBigDecimal("starting_price"),
                rs.getBigDecimal("current_price"),
                rs.getString("leader_username")
        );
    }
}
