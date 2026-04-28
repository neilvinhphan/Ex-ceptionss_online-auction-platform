package org.example.server.services;

import org.example.core.dto.CreateItemRequestDTO;
import org.example.core.models.items.Item;
import org.example.core.models.users.User;
import org.example.server.daos.ItemDAO;
import org.example.server.daos.UserDAO;
import org.example.server.validator.ItemCreationFactory;
import org.example.server.validator.ItemValidatorFactory;

import java.math.BigDecimal;

public class ItemService {
  private final ItemDAO itemDAO = ItemDAO.getInstance();
  private final UserDAO userDAO = UserDAO.getInstance();

  public Item createItem(String sellerUsername, Item item) throws Exception {
    if (sellerUsername == null || sellerUsername.trim().isEmpty()) {
      throw new Exception("Seller username is required.");
    }
    validateItemData(item);

    User seller = userDAO.getUserByUsername(sellerUsername);
    if (seller == null || seller.getUserName() == null) {
      throw new Exception("Seller not found.");
    }
    if (seller.getStatus().equals("BANNED")) {
      throw new Exception("Seller account is banned.");
    }

    Integer itemId = itemDAO.insertIntoItemTable(item);
    if (itemId == null || itemId <= 0) {
      throw new Exception("Cannot create item.");
    }
    itemDAO.insertIntoChildTable(item);
    return item;
  }

  public Item updateItemDescription(int itemId, String sellerUsername, String newDescription)
          throws Exception {
    if (itemId <= 0) {
      throw new Exception("Invalid item id.");
    }
    if (sellerUsername == null || sellerUsername.trim().isEmpty()) {
      throw new Exception("Seller username is required.");
    }
    if (newDescription == null || newDescription.trim().isEmpty()) {
      throw new Exception("Description cannot be empty.");
    }

    Item item = itemDAO.getItemById(itemId);
    if (item == null) {
      throw new Exception("Item not found.");
    }

    User seller = userDAO.getUserByUsername(sellerUsername);
    if (seller == null || seller.getUserName() == null) {
      throw new Exception("Seller not found.");
    }

    Integer ownerSellerId = itemDAO.getOwnerIdByItemId(itemId);
    if (ownerSellerId == null || ownerSellerId != seller.getUserId()) {
      throw new Exception("You are not allowed to update this item.");
    }

    String normalizedDescription = newDescription.trim();
    boolean success = itemDAO.updateItemDescriptionByItemId(itemId, normalizedDescription);
    if (!success) {
      throw new Exception("Cannot update item description.");
    }
    item.setDescription(normalizedDescription);
    return item;
  }

  private void validateItemData(Item item) throws Exception {
    if (item == null) {
      throw new Exception("Item data is required.");
    }
    if (item.getType() == null || item.getType().trim().isEmpty()) {
      throw new Exception("Item type is required.");
    }
    if (item.getItemName() == null || item.getItemName().trim().isEmpty()) {
      throw new Exception("Item name is required.");
    }
    BigDecimal startingPrice = item.getStartingPrice();
    if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
      throw new Exception("Starting price must be greater than zero.");
    }
    if (item.getDescription() == null || item.getDescription().trim().isEmpty()) {
      throw new Exception("Item description is required.");
    }
  }

  private void validateCommonData(CreateItemRequestDTO dto) throws Exception {
    if (dto == null) {
      throw new Exception("Dữ liệu sản phẩm không được để trống.");
    }
    if (dto.getType() == null || dto.getType().trim().isEmpty()) {
      throw new Exception("Loại danh mục sản phẩm là bắt buộc.");
    }
    if (dto.getItemName() == null || dto.getItemName().trim().isEmpty()) {
      throw new Exception("Tên sản phẩm là bắt buộc.");
    }
    if (dto.getStartingPrice() == null || dto.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new Exception("Giá khởi điểm phải lớn hơn 0.");
    }
    // Ông có thể thêm check Description ở đây nếu DTO cha có trường đó
  }

  public Item createItem(String sellerUsername, CreateItemRequestDTO requestDTO) throws Exception {
    // 1. Kiểm tra logic chung (Giá, Tên sản phẩm, Người bán tồn tại...)
    validateCommonData(requestDTO);

    // 2. Kích hoạt Strategy Pattern để kiểm tra thuộc tính riêng của từng subItem
    // Dù là Art hay Vehicle, chỉ cần gọi 1 dòng này!
    ItemValidatorFactory.validateSpecifics(requestDTO);

    // 3. Gọi Factory để tạo Object và ném xuống DAO lưu Database
    Item newItem = ItemCreationFactory.build(requestDTO);
    itemDAO.insertIntoItemTable(newItem);

    return newItem;
  }
}
