package org.example.server.daos;

import org.example.core.models.items.Item;
import org.example.core.shared.enums.AuctionStatus;
import org.example.server.config.DBConnection;
import org.example.core.models.entities.Auction;
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
            + "    ant.era, ant.material AS antique_material, ant.item_condition AS antique_condition, ant.is_certified,\n"
            + "    art.artist, art.creation_year,\n"
            + "    ele.brand AS ele_brand, ele.warranty_months, ele.item_condition AS ele_condition,\n"
            + "    jew.material AS jew_material, jew.gemstone, jew.weight AS jew_weight, jew.certification,\n"
            + "    re.location, re.area, re.property_type, re.legal_status,\n"
            + "    veh.brand AS veh_brand, veh.model, veh.manufacturing_year, veh.mileage,\n"
            + "    oth.category, oth.origin, oth.weight AS oth_weight\n"
            + "FROM items i\n"
            + "LEFT JOIN antique_items ant ON i.item_id = ant.item_id\n"
            + "LEFT JOIN art_items art ON i.item_id = art.item_id\n"
            + "LEFT JOIN electronics_items ele ON i.item_id = ele.item_id\n"
            + "LEFT JOIN jewelry_items jew ON i.item_id = jew.item_id\n"
            + "LEFT JOIN real_estate_items re ON i.item_id = re.item_id\n"
            + "LEFT JOIN vehicle_items veh ON i.item_id = veh.item_id\n"
            + "LEFT JOIN other_items oth ON i.item_id = oth.item_id\n"
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
            + "COALESCE(MAX(b.bid_amount), i.starting_price) AS highest_price "
            + "FROM auction a "
            + "JOIN items i ON a.item_id = i.item_id "
            + "LEFT JOIN bids b ON a.auction_id = b.auction_id "
            + "WHERE a.status = ? "
            + "GROUP BY a.auction_id";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, String.valueOf(status));
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        Auction auction = new Auction();
        auction.setAuctionId(rs.getInt("auction_id"));
        auction.setItemId(rs.getInt("item_id"));
        auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        auction.setHighestBid(rs.getBigDecimal("highest_price"));
        auctions.add(auction);
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return auctions;
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
          auction.setItemId(rs.getInt("item_id"));
          auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
          auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
          auction.setHighestBid(rs.getBigDecimal("highest_price"));
          return auction;
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public int getAuctionIdByItemId(int itemId) {
    String sql = "SELECT auction_id FROM auction WHERE items_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      return ps.executeQuery().getInt("id");
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updateNewAuctionItem(Item item, long time, BigDecimal bidIncrement) {
    String sql = "INSERT INTO auction_items (items_id, start_price, bid_increment, end_time) VALUES (?,?,?,?)";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, item.getId());
      ps.setBigDecimal(2, item.getStartingPrice());
      ps.setBigDecimal(3, bidIncrement);
      LocalDateTime endtime = LocalDateTime.now().plusMinutes(time);
      ps.setTimestamp(4, Timestamp.valueOf(endtime));
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void setAuctionStatus(int auctionId, AuctionStatus status) {
    String sql = "UPDATE auction_items SET status = ? WHERE id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, status.name());
      ps.setInt(2, auctionId);
      ps.executeUpdate();
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String getAuctionStatus(int auctionId) {
    String sql = "SELECT status FROM auction WHERE auction_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      return ps.executeQuery().getString("status");
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updateHighestPriceByItemId(int itemId, BigDecimal newPrice) {
    String sql = "UPDATE auction SET highest_price = ? WHERE item_id = ?";
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
