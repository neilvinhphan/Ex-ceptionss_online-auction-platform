package org.example.server.daos;

import org.example.core.models.items.AntiqueItem;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.JewelryItem;
import org.example.core.models.items.OtherItem;
import org.example.core.models.items.RealEstateItem;
import org.example.core.models.items.VehicleItem;
import org.example.core.shared.enums.ItemStatus;
import org.example.server.config.DBConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class ItemDAO {
  private static volatile ItemDAO instance = null;

  private ItemDAO() {}

  public static ItemDAO getInstance() {
    if (instance == null) {
      synchronized (ItemDAO.class) {
        if (instance == null) {
          instance = new ItemDAO();
        }
      }
    }
    return instance;
  }

  public List<Item> getAllItemByUserId(int id) {
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
            + "WHERE i.owner_id = ?;";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        List<Item> items = new java.util.ArrayList<>();
        while (rs.next()) {
          Item item = ItemFactory.takeItemFromDB(rs);
          //          item.setId(rs.getInt("item_id"));
          item.setSellerID(rs.getInt("owner_id"));
          item.setItemName(rs.getString("items_name"));
          item.setDescription(rs.getString("description"));
          item.setStartingPrice(rs.getBigDecimal("start_price"));
          items.add(item);
        }
        return items;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public int insertintoItemTable(Item item) {
    String sql =
        "INSERT INTO items (owner_id, items_name, description, start_price, type) VALUES (?,?,?,?,?)";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, item.getSellerID());
      ps.setString(2, item.getItemName());
      ps.setString(3, item.getDescription());
      ps.setBigDecimal(4, item.getStartingPrice());
      ps.setString(5, item.getType());
      int affectedRows = ps.executeUpdate();
      if (affectedRows != 0) {
        try (ResultSet rs = ps.getGeneratedKeys()) {
          if (rs.next()) {
            int itemId = rs.getInt(1);
            return itemId;
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return -1;
  }

  public boolean insertintoChildTable(Item item) {
    int itemId = insertintoItemTable(item);
    if (itemId == -1) {
      return false;
    }
    if (item.getType().equals("AntiqueItem")) {
      AntiqueItem antiqueItem = (AntiqueItem) item;
      String sql1 =
          "INSERT INTO antique_items (items_id, era, material, item_condition, is_certified) VALUES (?,?,?,?,?)";
      try (Connection connection = DBConnection.getConnection();
          PreparedStatement ps = connection.prepareStatement(sql1)) {
        ps.setInt(1, itemId);
        ps.setString(2, antiqueItem.getEra());
        ps.setString(3, antiqueItem.getMaterial());
        ps.setString(4, antiqueItem.getCondition());
        ps.setBoolean(5, antiqueItem.isCertified());
        return ps.executeUpdate() > 0;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    if (item.getType().equals("ArtItem")) {
      ArtItem artItem = (ArtItem) item;
      String sql2 = "INSERT INTO art_items (items_id, artist, creation_year) VALUES (?,?,?)";
      try (Connection connection = DBConnection.getConnection();
          PreparedStatement ps = connection.prepareStatement(sql2)) {
        ps.setInt(1, itemId);
        ps.setString(2, artItem.getArtist());
        ps.setInt(3, artItem.getCreationYear());
        return ps.executeUpdate() > 0;
      } catch (SQLException | IOException e) {
        throw new RuntimeException(e);
      }
    }
    if (item.getType().equals("ElectronicsItem")) {
      ElectronicsItem electronicsItem = (ElectronicsItem) item;
      String sql3 =
          "INSERT INTO electronics_items (items_id, brand, warranty_months, item_condition) VALUES (?,?,?,?)";
      try (Connection connection = DBConnection.getConnection();
          PreparedStatement ps = connection.prepareStatement(sql3)) {
        ps.setInt(1, itemId);
        ps.setString(2, electronicsItem.getBrand());
        ps.setInt(3, electronicsItem.getWarrantyMonths());
        ps.setString(4, electronicsItem.getCondition());
        return ps.executeUpdate() > 0;
      } catch (SQLException | IOException e) {
        throw new RuntimeException(e);
      }
    }
    if (item.getType().equals("JewelryItem")) {
      JewelryItem jewelryItem = (JewelryItem) item;
      String sql4 =
          "INSERT INTO jewelry_items (items_id, material, gemstone, weight, certification) VALUES (?,?,?,?,?)";
      try (Connection connection = DBConnection.getConnection();
          PreparedStatement ps = connection.prepareStatement(sql4)) {
        ps.setInt(1, itemId);
        ps.setString(2, jewelryItem.getMaterial());
        ps.setString(3, jewelryItem.getGemstone());
        ps.setDouble(4, jewelryItem.getWeight());
        ps.setString(5, jewelryItem.getCertification());
        return ps.executeUpdate() > 0;
      } catch (SQLException | IOException e) {
        throw new RuntimeException(e);
      }
    }
    if (item.getType().equals("RealEstateItem")) {
      RealEstateItem realEstateItem = (RealEstateItem) item;
      String sql5 =
          "INSERT INTO real_estate_items (items_id, location, area, property_type) VALUES (?,?,?,?)";
      try (Connection connection = DBConnection.getConnection();
          PreparedStatement ps = connection.prepareStatement(sql5)) {
        ps.setInt(1, itemId);
        ps.setString(2, realEstateItem.getLocation());
        ps.setDouble(3, realEstateItem.getArea());
        ps.setString(4, realEstateItem.getPropertyType());
        ps.setString(5, realEstateItem.getLegalStatus());
        return ps.executeUpdate() > 0;
      } catch (SQLException | IOException e) {
        throw new RuntimeException(e);
      }
    }
    if (item.getType().equals("VehicleItem")) {
      VehicleItem vehicleItem = (VehicleItem) item;
      String sql6 =
          "INSERT INTO vehicle_items (items_id, brand, model, manufacturing_year, mileage) VALUES (?,?,?,?,?)";
      try (Connection connection = DBConnection.getConnection();
          PreparedStatement ps = connection.prepareStatement(sql6)) {
        ps.setInt(1, itemId);
        ps.setString(2, vehicleItem.getBrand());
        ps.setString(3, vehicleItem.getModel());
        ps.setInt(4, vehicleItem.getManufacturingYear());
        ps.setDouble(5, vehicleItem.getMileage());
        return ps.executeUpdate() > 0;
      } catch (SQLException | IOException e) {
        throw new RuntimeException(e);
      }
    }
    if (item.getType().equals("OtherItem")) {
      OtherItem otherItem = (OtherItem) item;
      String sql7 = "INSERT INTO other_items (items_id, category, origin, weight) VALUES (?,?,?,?)";
      try (Connection connection = DBConnection.getConnection();
          PreparedStatement ps = connection.prepareStatement(sql7)) {
        ps.setInt(1, itemId);
        ps.setString(2, otherItem.getCategory());
        ps.setString(3, otherItem.getOrigin());
        ps.setDouble(4, otherItem.getWeight());
        return ps.executeUpdate() > 0;
      } catch (SQLException | IOException e) {
        throw new RuntimeException(e);
      }
    }
    return false;
  }

  public Item getItemById(int itemId) throws Exception {
    String sql =
        "SELECT item_id, item_type, item_name, description, starting_price FROM item WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          Item item = ItemFactory.takeItemFromDB(rs);
          //          item.setId(rs.getInt("item_id"));
          item.setItemName(rs.getString("item_name"));
          item.setDescription(rs.getString("description"));
          item.setStartingPrice(rs.getBigDecimal("starting_price"));
          return item;
        }
      }
    }
    return null;
  }

  public Integer getOwnerIdByItemId(int itemId) {
    String sql = "SELECT user_id FROM item WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("seller_id");
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public String getItemStatusById(int itemId) {
    String sql = "SELECT status FROM items WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
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

  public boolean updateOwnerIdInDB(int itemId, int ownerId) {
    String sql = "UPDATE items SET owner_id = ? WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, ownerId);
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
    }
    return false;
  }

  public boolean updateFinalPriceByItemId(int id) {
    String sql = "UPDATE items SET final_price = ? WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
    }
    return false;
  }

  public boolean updateItemDescription(int itemId, String description) {
    String sql = "UPDATE items SET description = ? WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, description);
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void updateItemStatus(int itemId, ItemStatus status) {
    String sql = "UPDATE items SET status = ? WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, status.name());
      ps.setInt(2, itemId);
      ps.executeUpdate();
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean deleteItem(int itemId) {
    String sql = "DELETE FROM items WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
