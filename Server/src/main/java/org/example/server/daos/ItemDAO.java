package org.example.server.daos;

import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.ItemFactory;
import org.example.core.models.items.VehicleItem;
import org.example.core.shared.enums.ItemStatus;
import org.example.server.config.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lớp truy cập dữ liệu (DAO) thực hiện các thao tác CRUD trên bảng sản phẩm (Items),
 * hỗ trợ ánh xạ dữ liệu đa hình (Inheritance mapping) xuống các bảng con chuyên biệt.
 */
public class ItemDAO {
  private static final Logger logger = Logger.getLogger(ItemDAO.class.getName());
  private static volatile ItemDAO instance = null;

  private ItemDAO() {}

  /**
   * Lấy instance duy nhất (Singleton) của ItemDAO (Thread-safe).
   */
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

  // --- NHÓM PHƯƠNG THỨC GHI DỮ LIỆU (WRITE) ---

  /**
   * Chèn thông tin chung của sản phẩm vào bảng items chính.
   * * @return ID tự tăng (Primary Key) của item vừa tạo, hoặc -1 nếu thất bại.
   */
  public int insertIntoItemTable(Item item) {
    String sql = "INSERT INTO items (owner_id, items_name, description, start_price, type, image, status) VALUES (?,?,?,?,?,?,?)";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, item.getSellerID());
      ps.setString(2, item.getItemName());
      ps.setString(3, item.getDescription());
      ps.setBigDecimal(4, item.getStartingPrice());
      ps.setString(5, item.getType());
      ps.setString(6, item.getImage());
      ps.setString(7, item.getStatus().name());

      if (ps.executeUpdate() > 0) {
        try (ResultSet rs = ps.getGeneratedKeys()) {
          if (rs.next()) {
            return rs.getInt(1);
          }
        }
      }
      return -1;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi khi chèn dữ liệu vào bảng items chính", e);
      throw new RuntimeException("Lưu sản phẩm thất bại", e);
    }
  }

  /**
   * Ép kiểu đa hình và lưu thông tin thuộc tính mở rộng của sản phẩm vào đúng bảng con tương ứng.
   */
  public boolean insertIntoChildTable(Item item, int itemId) {
    if ("ART".equalsIgnoreCase(item.getType())) {
      ArtItem artItem = (ArtItem) item;
      String sql = "INSERT INTO art_items (items_id, artist, creation_year) VALUES (?,?,?)";
      try (Connection connection = DBConnection.getConnection();
           PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setInt(1, itemId);
        ps.setString(2, artItem.getArtist());
        ps.setInt(3, artItem.getCreationYear());
        return ps.executeUpdate() > 0;
      } catch (SQLException e) {
        logger.log(Level.SEVERE, "Lỗi chèn thông tin sản phẩm Nghệ thuật vào bảng art_items. Item ID: " + itemId, e);
        throw new RuntimeException("Lưu thuộc tính sản phẩm Nghệ thuật thất bại", e);
      }
    }

    if ("ELECTRONICS".equalsIgnoreCase(item.getType())) {
      ElectronicsItem electronicsItem = (ElectronicsItem) item;
      String sql = "INSERT INTO electronics_items (items_id, ele_brand, warranty_months, item_condition) VALUES (?,?,?,?)";
      try (Connection connection = DBConnection.getConnection();
           PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setInt(1, itemId);
        ps.setString(2, electronicsItem.getBrand());
        ps.setInt(3, electronicsItem.getWarrantyMonths());
        ps.setString(4, electronicsItem.getCondition());
        return ps.executeUpdate() > 0;
      } catch (SQLException e) {
        logger.log(Level.SEVERE, "Lỗi chèn sản phẩm Điện tử vào bảng electronics_items. Item ID: " + itemId, e);
        throw new RuntimeException("Lưu thuộc tính sản phẩm Điện tử thất bại", e);
      }
    }

    if ("VEHICLE".equalsIgnoreCase(item.getType())) {
      VehicleItem vehicleItem = (VehicleItem) item;
      String sql = "INSERT INTO vehicle_items (items_id, veh_brand, model, manufacturing_year, mileage) VALUES (?,?,?,?,?)";
      try (Connection connection = DBConnection.getConnection();
           PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setInt(1, itemId);
        ps.setString(2, vehicleItem.getBrand());
        ps.setString(3, vehicleItem.getModel());
        ps.setInt(4, vehicleItem.getManufacturingYear());
        ps.setDouble(5, vehicleItem.getMileage());
        return ps.executeUpdate() > 0;
      } catch (SQLException e) {
        logger.log(Level.SEVERE, "Lỗi chèn sản phẩm Xe cộ vào bảng vehicle_items. Item ID: " + itemId, e);
        throw new RuntimeException("Lưu thuộc tính sản phẩm Phương tiện thất bại", e);
      }
    }
    return false;
  }

  public boolean updateOwnerIdByItemId(int itemId, int userId) {
    String sql = "UPDATE items SET owner_id = ? WHERE items_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi cập nhật chủ sở hữu mới cho Item ID: " + itemId, e);
      throw new RuntimeException("Cập nhật chủ sở hữu sản phẩm thất bại", e);
    }
  }

  public boolean updateStartPriceByItemId(int itemId, BigDecimal startPrice) {
    String sql = "UPDATE items SET start_price = ? WHERE items_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setBigDecimal(1, startPrice);
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi cập nhật giá khởi điểm cho Item ID: " + itemId, e);
      throw new RuntimeException("Cập nhật giá khởi điểm thất bại", e);
    }
  }

  public boolean updateItemDescriptionByItemId(int itemId, String description) {
    String sql = "UPDATE items SET description = ? WHERE items_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, description);
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi cập nhật mô tả cho Item ID: " + itemId, e);
      throw new RuntimeException("Cập nhật mô tả sản phẩm thất bại", e);
    }
  }

  public boolean updateItemStatus(int itemId, ItemStatus status) {
    String sql = "UPDATE items SET status = ? WHERE items_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, status.name());
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi cập nhật trạng thái kiểm duyệt cho Item ID: " + itemId, e);
      throw new RuntimeException("Cập nhật trạng thái sản phẩm thất bại", e);
    }
  }

  public boolean updateItemNameByItemId(int itemId, String name) {
    String sql = "UPDATE items SET items_name = ? WHERE items_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, name);
      ps.setInt(2, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi cập nhật tiêu đề tên sản phẩm cho Item ID: " + itemId, e);
      throw new RuntimeException("Cập nhật tên sản phẩm thất bại", e);
    }
  }

  /**
   * Lưu kết quả thẩm định định giá tài sản tự động của hệ thống AI vào cơ sở dữ liệu.
   */
  public boolean updateAiEvaluation(Item item) {
    String sql = "UPDATE items SET status = ?, suggested_price = ?, ai_reason = ? WHERE items_id = ?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, item.getStatus().name());
      ps.setBigDecimal(2, item.getSuggestedPrice());
      ps.setString(3, item.getAiReason());
      ps.setInt(4, item.getItemId());
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi khi đồng bộ kết quả kiểm duyệt AI cho Item ID: " + item.getItemId(), e);
      return false;
    }
  }

  public boolean deleteItemByItemId(int itemId) {
    String sql = "DELETE FROM items WHERE items_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi xóa bản ghi sản phẩm khỏi hệ thống. Item ID: " + itemId, e);
      throw new RuntimeException("Xóa tài sản thất bại", e);
    }
  }

  // --- NHÓM PHƯƠNG THỨC TRUY VẤN DỮ LIỆU (READ) ---

  /**
   * Truy vấn thông tin chi tiết một tài sản bằng ID, thực hiện JOIN toàn bộ bảng con để nặn Object đa hình hoàn chỉnh.
   */
  public Item getItemById(int itemId) {
    String sql = """
        SELECT i.*, art.artist, art.creation_year, ele.ele_brand, ele.warranty_months, ele.item_condition,
               veh.veh_brand, veh.model, veh.manufacturing_year, veh.mileage
        FROM items i LEFT JOIN art_items art ON i.items_id = art.items_id
        LEFT JOIN electronics_items ele ON i.items_id = ele.items_id
        LEFT JOIN vehicle_items veh ON i.items_id = veh.items_id WHERE i.items_id = ?
        """;
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          Item item = ItemFactory.takeItemFromDB(rs);
          item.setItemId(rs.getInt("items_id"));
          item.setSellerID(rs.getInt("owner_id"));
          item.setItemName(rs.getString("items_name"));
          item.setDescription(rs.getString("description"));
          item.setStartingPrice(rs.getBigDecimal("start_price"));
          item.setImage(rs.getString("image"));
          item.setStatus(ItemStatus.valueOf(rs.getString("status")));
          item.setSuggestedPrice(rs.getBigDecimal("suggested_price"));
          item.setAiReason(rs.getString("ai_reason"));
          return item;
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi truy vấn chi tiết sản phẩm ID: " + itemId, e);
      throw new RuntimeException("Tải thông tin sản phẩm thất bại", e);
    }
    return null;
  }

  /**
   * Lấy mã ID người sở hữu (Seller) hiện tại của sản phẩm.
   */
  public Integer getOwnerIdByItemId(int itemId) {
    String sql = "SELECT owner_id FROM items WHERE items_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("owner_id");
        }
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi lấy ID chủ sở hữu của tài sản ID: " + itemId, e);
      throw new RuntimeException("Truy vấn chủ sở hữu thất bại", e);
    }
    return null;
  }

  /**
   * Lấy chuỗi văn bản tên của sản phẩm.
   */
  public String getItemNameByItemId(int itemId) {
    String sql = "SELECT items_name FROM items WHERE items_id = ?";
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("items_name");
        }
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Lỗi lấy tên sản phẩm của tài sản ID: " + itemId, e);
      throw new RuntimeException("Truy vấn tiêu đề sản phẩm thất bại", e);
    }
    return null;
  }

  /**
   * Lấy toàn bộ danh sách kho đồ cá nhân thuộc sở hữu của một tài khoản cụ thể.
   */
  public List<Item> getAllItemByUserId(int userId) {
    List<Item> items = new ArrayList<>();
    String sql = """
        SELECT i.*, art.artist, art.creation_year, ele.ele_brand, ele.warranty_months, ele.item_condition,
               veh.veh_brand, veh.model, veh.manufacturing_year, veh.mileage
        FROM items i LEFT JOIN art_items art ON i.items_id = art.items_id
        LEFT JOIN electronics_items ele ON i.items_id = ele.items_id
        LEFT JOIN vehicle_items veh ON i.items_id = veh.items_id WHERE i.owner_id = ?
        """;
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Item item = ItemFactory.takeItemFromDB(rs);
          item.setItemId(rs.getInt("items_id"));
          item.setImage(rs.getString("image"));
          item.setSellerID(rs.getInt("owner_id"));
          item.setItemName(rs.getString("items_name"));
          item.setDescription(rs.getString("description"));
          item.setStartingPrice(rs.getBigDecimal("start_price"));
          item.setStatus(ItemStatus.valueOf(rs.getString("status")));
          item.setSuggestedPrice(rs.getBigDecimal("suggested_price"));
          item.setAiReason(rs.getString("ai_reason"));
          items.add(item);
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi tải danh sách kho đồ của User ID: " + userId, e);
      throw new RuntimeException("Tải danh sách kho đồ cá nhân thất bại", e);
    }
    return items;
  }

  /**
   * Lọc và kết xuất toàn bộ sản phẩm trên hệ thống dựa theo một bộ lọc trạng thái kiểm duyệt (ItemStatus).
   */
  public List<Item> getItemsByStatus(ItemStatus status) {
    List<Item> items = new ArrayList<>();
    String sql = """
        SELECT i.*, art.artist, art.creation_year, ele.ele_brand, ele.warranty_months, ele.item_condition,
               veh.veh_brand, veh.model, veh.manufacturing_year, veh.mileage
        FROM items i LEFT JOIN art_items art ON i.items_id = art.items_id
        LEFT JOIN electronics_items ele ON i.items_id = ele.items_id
        LEFT JOIN vehicle_items veh ON i.items_id = veh.items_id WHERE i.status = ?
        """;
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, status.name());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Item item = ItemFactory.takeItemFromDB(rs);
          item.setItemId(rs.getInt("items_id"));
          item.setImage(rs.getString("image"));
          item.setSellerID(rs.getInt("owner_id"));
          item.setItemName(rs.getString("items_name"));
          item.setDescription(rs.getString("description"));
          item.setStartingPrice(rs.getBigDecimal("start_price"));
          item.setStatus(ItemStatus.valueOf(rs.getString("status")));
          item.setSuggestedPrice(rs.getBigDecimal("suggested_price"));
          item.setAiReason(rs.getString("ai_reason"));
          items.add(item);
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi lọc danh sách sản phẩm theo trạng thái kiểm duyệt: " + status, e);
      throw new RuntimeException("Tải danh sách lọc sản phẩm thất bại", e);
    }
    return items;
  }

  /**
   * Chốt chặn an toàn: Chỉ trích xuất danh sách các mặt hàng đã được phê duyệt (APPROVED) của riêng một Người bán.
   */
  public List<Item> getApprovedItemsByUserId(int userId) {
    List<Item> items = new ArrayList<>();
    String sql = """
        SELECT i.*, art.artist, art.creation_year, ele.ele_brand, ele.warranty_months, ele.item_condition,
               veh.veh_brand, veh.model, veh.manufacturing_year, veh.mileage
        FROM items i LEFT JOIN art_items art ON i.items_id = art.items_id
        LEFT JOIN electronics_items ele ON i.items_id = ele.items_id
        LEFT JOIN vehicle_items veh ON i.items_id = veh.items_id WHERE i.owner_id = ? AND i.status = 'APPROVED'
        """;
    try (Connection connection = DBConnection.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Item item = ItemFactory.takeItemFromDB(rs);
          item.setItemId(rs.getInt("items_id"));
          item.setImage(rs.getString("image"));
          item.setSellerID(rs.getInt("owner_id"));
          item.setItemName(rs.getString("items_name"));
          item.setDescription(rs.getString("description"));
          item.setStartingPrice(rs.getBigDecimal("start_price"));
          item.setStatus(ItemStatus.valueOf(rs.getString("status")));
          item.setSuggestedPrice(rs.getBigDecimal("suggested_price"));
          item.setAiReason(rs.getString("ai_reason"));
          items.add(item);
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi lấy danh sách đồ APPROVED của User ID: " + userId, e);
      throw new RuntimeException("Tải danh sách sản phẩm khả dụng thất bại", e);
    }
    return items;
  }
}