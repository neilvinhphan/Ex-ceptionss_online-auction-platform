package org.example.server.services;

import org.example.core.dto.CreateItemRequestDTO;
import org.example.core.dto.DeleteRequestDTO;
import org.example.core.dto.EditProductRequestDTO;
import org.example.core.dto.PendingItemsDTO;
import org.example.core.models.items.Item;
import org.example.core.models.items.ItemFactory;
import org.example.server.daos.ItemDAO;
import org.example.server.daos.UserDAO;

import java.math.BigDecimal;
import java.util.List;

public class ItemService {
  private static final ItemDAO itemDAO = ItemDAO.getInstance();
  private static final UserDAO userDAO = UserDAO.getInstance();

  public static Item createItem(CreateItemRequestDTO requestPayload) throws Exception {

    validateItemData(requestPayload);

    Item newItem = ItemFactory.createItemDTO(requestPayload);

    Integer itemId = itemDAO.insertIntoItemTable(newItem);
    if (itemId == null || itemId <= 0) {
      throw new Exception("Cannot create item.");
    }
    itemDAO.insertIntoChildTable(newItem, itemId);
    return newItem;
  }

  public static boolean updateItemFull(EditProductRequestDTO requestPayload) {
    int itemId = requestPayload.getItemId();
    String newName = requestPayload.getItemEditName();
    String newDescription = requestPayload.getDescription();
    BigDecimal newPrice = requestPayload.getPrice();

    if (itemDAO.updateItemDescriptionByItemId(itemId, newDescription)
        && itemDAO.updateItemNameByItemId(itemId, newName)
        && itemDAO.updateStartPriceByItemId(itemId, newPrice)) {
      return true;
    } else {
      return false;
    }
  }

  public static List<Item> getAllItem(PendingItemsDTO requestPayload) throws Exception {
    int sellerId = requestPayload.getSellerId();
    if (sellerId <= 0) {
      throw new Exception("Invalid seller ID.");
    }
    List<Item> items = itemDAO.getAllItemByUserId(sellerId);
    return items;
  }

  public static boolean deleteItem(DeleteRequestDTO requestPayload) throws Exception {
    int itemId = requestPayload.getItemId();
    if (itemId <= 0) {
      throw new Exception("Invalid item ID.");
    }
    boolean success = itemDAO.deleteItemByItemId(itemId);
    if (!success) {
      throw new Exception("Cannot delete item.");
    }
    return true;
  }

  private static void validateItemData(CreateItemRequestDTO item) throws Exception {
    if (item == null) {
      throw new Exception("Item data is required.");
    }
    if (item.getSellerID() <= 0) {
      throw new Exception("Invalid seller ID.");
    }
    if (item.getType() == null || item.getType().trim().isEmpty()) {
      throw new Exception("Item type is required.");
    }
    if (item.getItemName() == null || item.getItemName().trim().isEmpty()) {
      throw new Exception("Item name is required.");
    }
    if (item.getDescription() == null || item.getDescription().trim().isEmpty()) {
      throw new Exception("Item description is required.");
    }
    if (item.getStartingPrice() == null
        || item.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new Exception("Starting price must be greater than zero.");
    }
    if (item.getBase64Image() == null || item.getBase64Image().trim().isEmpty()) {
      throw new Exception("Item image is required.");
    }
  }
}
