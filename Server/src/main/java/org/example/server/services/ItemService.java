package org.example.server.services;

import org.example.core.models.items.Item;
import org.example.core.models.users.User;
import org.example.server.daos.ItemDAO;
import org.example.server.daos.UserDAO;

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
    if (item.getDescription() == null || item.getDescription().trim().isEmpty()) {
      throw new Exception("Item description is required.");
    }
  }
}
