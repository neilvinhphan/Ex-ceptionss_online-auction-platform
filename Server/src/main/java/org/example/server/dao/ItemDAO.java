package org.example.server.dao;

import org.example.core.models.items.AntiqueItem;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.JewelryItem;
import org.example.core.models.items.OtherItem;
import org.example.core.models.items.RealEstateItem;
import org.example.core.models.items.VehicleItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ItemDAO {
    private static ItemDAO instance = null;
    private ItemDAO() {}
    public static ItemDAO getInstance() {
        if (instance == null) {
            synchronized (ItemDAO.class) {
                if (instance == null) {
                    instance = new ItemDAO();
                }
            }
        } return instance;
    }
    public int insertintoItemTable(Item item) {
        String sql = "INSERT INTO items (owner_id, items_name, description, start_price, type) VALUES (?,?,?,?,?)";
        try(Connection connection = DBConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, item.getOwnerId());
            ps.setString(2, item.getItemName());
            ps.setString(3, item.getDescription());
            ps.setBigDecimal(4, item.getStartingPrice());
            ps.setString(5, item.getType());
            int affectedRows = ps.executeUpdate();
            if (affectedRows != 0) {
                try(ResultSet rs = ps.getGeneratedKeys()) {
                    if(rs.next()) {
                        int itemId = rs.getInt(1);
                        return itemId;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } return -1;
    }

  public boolean insertintoChildTable(Item item) {
    int itemId = insertintoItemTable(item);
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
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      if (item.getType().equals("ArtItem")) {
        ArtItem artItem = (ArtItem) item;
        String sql2 = "INSERT INTO art_items (items_id, artist, ";
      }
    } throw new RuntimeException();
        }


        public Integer createItem(int ownerId, Item item) throws Exception {
        String sql = "INSERT INTO items (item_type, item_name, description, starting_price, owner_id) VALUES (?,?,?,?,?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getType());
            ps.setString(2, item.getItemName());
            ps.setString(3, item.getDescription());
            ps.setBigDecimal(4, item.getStartingPrice());
            ps.setInt(5, ownerId);
            if (ps.executeUpdate() <= 0) {
                return null;
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return null;
        }
    }

    public Item getItemById(int itemId) throws Exception {
        String sql =
                "SELECT item_id, item_type, item_name, description, starting_price FROM item WHERE item_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Item item = ItemFactory.createItem(rs.getString("item_type"));
                    item.setId(rs.getInt("item_id"));
                    item.setItemName(rs.getString("item_name"));
                    item.setDescription(rs.getString("description"));
                    item.setStartingPrice(rs.getBigDecimal("starting_price"));
                    return item;
                }
            }
        }
        return null;
    }

    public Integer getSellerIdByItemId(int itemId) throws Exception {
        String sql = "SELECT seller_id FROM item WHERE item_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("seller_id");
                }
            }
        }
        return null;
    }

    public boolean updateItemDescription(int itemId, String description) throws Exception {
        String sql = "UPDATE item SET description = ? WHERE item_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, description);
            ps.setInt(2, itemId);
            return ps.executeUpdate() > 0;
        }
    }

}
class ItemFactory {
    public static Item createItem(String type) {
        return switch (type) {
            case "ArtItem" -> new ArtItem();
            case "ElectronicsItem" -> new ElectronicsItem();
            case "AntiqueItem" -> new AntiqueItem();
            case "JewelryItem" -> new JewelryItem();
            case "RealEstateItem" -> new RealEstateItem();
            case "VehicleItem" -> new VehicleItem();
            default -> new OtherItem();
        };
    }
}