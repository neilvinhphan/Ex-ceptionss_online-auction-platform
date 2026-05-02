package org.example.server.daos;

import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.ItemFactory;
import org.example.core.models.items.VehicleItem;
import org.example.core.shared.enums.ItemStatus;
import org.example.server.config.DBConnection;

import java.io.IOException;
import java.math.BigDecimal;
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

  public List<Item> getAllItemByUserId(int userId) {
    String sql = """
    SELECT 
        i.*, 
        art.artist, art.creation_year, 
        ele.brand AS ele_brand, ele.warranty_months, ele.item_condition,
        veh.brand AS veh_brand, veh.model, veh.manufacturing_year, veh.mileage
    FROM items i
    LEFT JOIN art_items art ON i.items_id = art.items_id
    LEFT JOIN electronics_items ele ON i.items_id = ele.items_id
    LEFT JOIN vehicle_items veh ON i.items_id = veh.items_id
    WHERE i.owner_id = ?
    """;
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        List<Item> items = new java.util.ArrayList<>();
        while (rs.next()) {
          Item item = ItemFactory.takeItemFromDB(rs);
          item.setItemId(rs.getInt("items_id"));
          item.setSellerID(rs.getInt("owner_id"));
          item.setItemName(rs.getString("items_name"));
          item.setDescription(rs.getString("description"));
          item.setStartingPrice(rs.getBigDecimal("start_price"));
          // Lấy trạng thái từ DB ép vào Object Item
          String dbStatus = rs.getString("status");
          if (dbStatus != null && !dbStatus.trim().isEmpty()) {
            item.setStatus(ItemStatus.valueOf(dbStatus));
          } else {
            item.setStatus(ItemStatus.DRAFT);
          }
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

  public int insertIntoItemTable(Item item) {
    String sql =
            "INSERT INTO items (owner_id, items_name, description, start_price, type) VALUES (?,?,?,?,?)";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

  public boolean insertIntoChildTable(Item item, int itemId) {

    if (item.getType().equals("ART")) {
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
    if (item.getType().equals("ELECTRONICS")) {
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
    if (item.getType().equals("VEHICLE")) {
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
    return false;
  }

  public Item getItemById(int itemId) {
    String sql =
            "SELECT item_id, owner_id, type, items_name, description, start_price FROM items WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          Item item = ItemFactory.takeItemFromDB(rs);
          item.setItemId(rs.getInt("item_id"));
          item.setSellerID(rs.getInt("owner_id"));
          item.setItemName(rs.getString("item_name"));
          item.setDescription(rs.getString("description"));
          item.setStartingPrice(rs.getBigDecimal("starting_price"));
          return item;
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public Integer getOwnerIdByItemId(int itemId) {
    String sql = "SELECT owner_id FROM items WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("owner_id");
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public String getItemTypeByItemId(int itemId) {
    String sql = "SELECT type FROM items WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("type");
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public ItemStatus getItemStatusById(int itemId) {
    String sql = "SELECT status FROM items WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return ItemStatus.valueOf(rs.getString("status"));
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public int getItemIdByItemName(String itemName) {
    String sql = "SELECT item_id FROM items WHERE items_name = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, itemName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("item_id");
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    return -1;
  }

  public boolean updateOwnerIdByItemId(int itemId, int userId) {
    String sql = "UPDATE items SET owner_id = ? WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updateStartPriceByItemId(int itemId, BigDecimal startPrice) {
    String sql = "UPDATE items SET start_price = ? WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setBigDecimal(1, startPrice);
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updateOwnerIdInDB(int itemId, int ownerId) {
    String sql = "UPDATE items SET owner_id = ? WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, ownerId);
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean updateFinalPriceByItemId(int id, BigDecimal finalPrice) {
    String sql = "UPDATE items SET final_price = ? WHERE item_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setBigDecimal(1, finalPrice);
      ps.setInt(2, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException | IOException e) {
    }
    return false;
  }

  public boolean updateItemDescriptionByItemId(int itemId, String description) {
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

  public boolean updateItemStatus(int itemId, ItemStatus status) {
    String sql = "UPDATE items SET status = ? WHERE items_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, status.name());
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
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