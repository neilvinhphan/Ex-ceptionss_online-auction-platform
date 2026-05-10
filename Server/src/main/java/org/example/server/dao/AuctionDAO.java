package org.example.server.daos;

import org.example.core.models.entities.BidTransaction;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.ItemFactory;
import org.example.core.models.items.VehicleItem;
import org.example.core.shared.enums.AuctionStatus;
import org.example.server.config.DBConnection;
import org.example.core.models.entities.Auction;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {
  private static volatile AuctionDAO instance;

  private AuctionDAO() {}

  public static AuctionDAO getInstance() {
    if (instance == null) {
      synchronized (AuctionDAO.class) {
        if (instance == null) {
          instance = new AuctionDAO();
        }
      }
    }
    return instance;
  }

  public List<Item> getAllItemByStatus(AuctionStatus status) {
    List<Item> items = new ArrayList<>();
    String sql =
        "SELECT \n"
            + "    i.*, \n"
            + "    art.artist, art.creation_year,\n"
            + "    ele.brand AS ele_brand, ele.warranty_months, ele.item_condition AS ele_condition,\n"
            + "    veh.brand AS veh_brand, veh.model, veh.manufacturing_year, veh.mileage\n"
            + "FROM items i\n"
            + "LEFT JOIN art_items art ON i.items_id = art.items_id\n"
            + "LEFT JOIN electronics_items ele ON i.items_id = ele.items_id\n"
            + "LEFT JOIN vehicle_items veh ON i.items_id = veh.items_id\n"
            + "WHERE i.status = ?\n";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, status.name());
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        Item item = ItemFactory.takeItemFromDB(rs);
        items.add(item);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return items;
  }

  public List<Auction> getAllAuctionsByStatus(AuctionStatus status) {
    List<Auction> auctions = new ArrayList<>();
    String sql =
        "SELECT a.*, "
            + "COALESCE(MAX(b.bid_amount), i.start_price) AS highest_price "
            + "FROM auction a "
            + "JOIN items i ON a.items_id = i.items_id "
            + "LEFT JOIN bid b ON a.auction_id = b.auction_id "
            + "WHERE a.status = ? "
            + "GROUP BY a.auction_id";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, String.valueOf(status));
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        Auction auction = new Auction();
        auction.setAuctionId(rs.getInt("auction_id"));
        auction.setItemId(rs.getInt("items_id"));
        auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        auction.setHighestBid(rs.getBigDecimal("highest_price"));
        auction.setBidIncrement(rs.getBigDecimal("bid_increment"));
        auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        auctions.add(auction);
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return auctions;
  }

  public List<Auction> getAllAuctionsByStatusForCatalog(AuctionStatus status) {
    List<Auction> auctions = new ArrayList<>();

    // 1. Cập nhật câu SQL: Lấy thêm tên, loại, và giá khởi điểm của Item
    String sql =
        "SELECT a.auction_id, a.items_id, a.start_time, a.end_time, a.status, a.bid_increment, a.bidder_id,"
            + "i.items_name AS item_name, i.type AS item_type, i.start_price, i.image, "
            + "COALESCE(MAX(b.bid_amount), i.start_price) AS highest_price "
            + "FROM auction a "
            + "JOIN items i ON a.items_id = i.items_id "
            + "LEFT JOIN bid b ON a.auction_id = b.auction_id "
            + "WHERE a.status = ? "
            + "GROUP BY a.auction_id, a.items_id, a.start_time, a.end_time, a.status, "
            + "i.items_name, i.type, i.start_price, i.image";

    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {

      ps.setString(1, String.valueOf(status));
      ResultSet rs = ps.executeQuery();

      while (rs.next()) {
        Auction auction = new Auction();
        auction.setAuctionId(rs.getInt("auction_id"));
        auction.setItemId(rs.getInt("items_id"));
        auction.setBidderId(rs.getInt("bidder_id"));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
          auction.setStatus(AuctionStatus.valueOf(statusStr.toUpperCase()));
        }
        auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        auction.setHighestBid(rs.getBigDecimal("highest_price"));
        auction.setBidIncrement(rs.getBigDecimal("bid_increment"));
        auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        List<BidTransaction> bidTransactions = BidDAO.getInstance().getBidTransactionByAuctionId(auction.getAuctionId());
        auction.setBidHistory(bidTransactions);

        // 2. Bóc tách dữ liệu Item và khởi tạo object đa hình
        String itemType = rs.getString("item_type");
        Item item = null;

        if (itemType != null) {
          switch (itemType.toUpperCase()) {
            case "ART" -> item = new ArtItem();
            case "ELECTRONICS" -> item = new ElectronicsItem();
            case "VEHICLE" -> item = new VehicleItem();
              // Nếu sau này có thêm loại nào thì add case vào đây
          }

          // Set các thuộc tính chung của Item
          if (item != null) {
            item.setItemId(rs.getInt("items_id"));
            item.setItemName(rs.getString("item_name"));
            item.setType(itemType);
            item.setStartingPrice(rs.getBigDecimal("start_price"));
            item.setImage(rs.getString("image"));

            // Gắn Item hoàn chỉnh vào Auction
            auction.setItem(item);
          }
        }

        auctions.add(auction);
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }

    return auctions;
  }

  public int getAuctionIdByItemId(int itemId) {
    String sql = "SELECT auction_id FROM auction WHERE items_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("auction_id");
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return -1;
  }

  public int createNewAuctionItem(Item item, long time, BigDecimal bidIncrement) {
    String sql =
        "INSERT INTO auction (items_id, start_price, bid_increment, end_time) VALUES (?,?,?,?)";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, item.getItemId());
      ps.setBigDecimal(2, item.getStartingPrice());
      ps.setBigDecimal(3, bidIncrement);
      LocalDateTime endtime = LocalDateTime.now().plusMinutes(time);
      ps.setTimestamp(4, Timestamp.valueOf(endtime));
      try (ResultSet rs = ps.executeUpdate() > 0 ? ps.getGeneratedKeys() : null) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return -1;
  }

  public void setAuctionStatus(int auctionId, AuctionStatus status) {
    String sql = "UPDATE auction SET status = ? WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, status.name());
      ps.setInt(2, auctionId);
      ps.executeUpdate();
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Auction getAuctionByAuctionId(int auctionId) {
    String sql = "SELECT * FROM auction WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          Auction auction = new Auction();
          auction.setAuctionId(rs.getInt("auction_id"));
          auction.setItemId(rs.getInt("items_id"));
          auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
          auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
          auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
          auction.setBidIncrement(rs.getBigDecimal("bid_increment"));
          auction.setHighestBid(rs.getBigDecimal("highest_price"));
          auction.setId(rs.getInt("bidder_id"));
          return auction;
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public BigDecimal getBidIncrementByAuctionId(int auctionId) {
    String sql = "SELECT bid_increment FROM auction WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getBigDecimal("bid_increment");
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public boolean updateAuctionEndTime(int auctionId, LocalDateTime endTime) {
    String sql = "UPDATE auction SET end_time = ? WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setTimestamp(1, Timestamp.valueOf(endTime));
      ps.setInt(2, auctionId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String getAuctionStatus(int auctionId) {
    String sql = "SELECT status FROM auction WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("status");
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public boolean updateHighestPriceByItemId(int itemId, BigDecimal newPrice) {
    String sql = "UPDATE auction SET highest_price = ? WHERE items_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setBigDecimal(1, newPrice);
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
