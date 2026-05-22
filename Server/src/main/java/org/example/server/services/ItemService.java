package org.example.server.services;

import org.example.core.dto.admin.AiEvaluationDTO;
import org.example.core.dto.itemsDTO.CreateItemRequestDTO;
import org.example.core.dto.itemsDTO.DeleteRequestDTO;
import org.example.core.dto.itemsDTO.EditProductRequestDTO;
import org.example.core.dto.itemsDTO.PendingItemsDTO;
import org.example.core.models.items.Item;
import org.example.core.models.items.ItemFactory;
import org.example.core.shared.enums.ItemStatus;
import org.example.server.daos.ItemDAO;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ItemService {
  private final ItemDAO itemDAO;
  private static volatile ItemService instance = null;

  ItemService(ItemDAO itemDAO) {
    this.itemDAO = itemDAO;
  }

  public static ItemService getInstance() {
    if (instance == null) {
      synchronized (ItemService.class) {
        if (instance == null) {
          instance = new ItemService(ItemDAO.getInstance());
        }
      }
    }
    return instance;
  }

  /** TẠO SẢN PHẨM MỚI TÍCH HỢP AI KIỂM DUYỆT */
  public Item createItem(CreateItemRequestDTO requestPayload) throws Exception {

    validateItemData(requestPayload);

    Item newItem = ItemFactory.createItemDTO(requestPayload);

    Integer itemId = itemDAO.insertIntoItemTable(newItem);
    if (itemId == null || itemId <= 0) {
      throw new Exception("Cannot create item.");
    }

    newItem.setItemId(itemId); // Gán ID để tí nữa luồng AI biết đường mà UPDATE
    itemDAO.insertIntoChildTable(newItem, itemId);

    // 2. 🚀 CHẠY NGẦM (ASYNC): Gọi AI để thẩm định nội dung và định giá
    CompletableFuture.runAsync(
        () -> {
          try {
            System.out.println("🤖 AI đang bắt đầu quét vật phẩm ID: " + itemId);

            // Gọi trạm AI Moderation (LM Studio hoặc Gemini)
            AiEvaluationDTO evaluation = AiModerationService.evaluateItem(newItem);

            // Cập nhật kết quả AI trả về vào đối tượng
            newItem.setSuggestedPrice(evaluation.getSuggestedPrice());
            newItem.setAiReason(evaluation.getReason());

            // Nếu AI xác nhận an toàn (isSafe = true), tự động nâng cấp status lên APPROVED
            if (evaluation.isSafe()) {
              newItem.setStatus(ItemStatus.APPROVED);
              System.out.println("✅ AI tự động DUYỆT vật phẩm ID: " + itemId);
            } else {
              System.out.println(
                  "🚩 AI phát hiện nghi vấn tại ID: "
                      + itemId
                      + ". Giữ trạng thái PENDING cho Admin.");
            }

            // Lưu kết quả thẩm định cuối cùng của AI vào Database
            itemDAO.updateAiEvaluation(newItem);

          } catch (Exception e) {
            System.err.println("❌ Lỗi trong quá trình AI thẩm định: " + e.getMessage());
          }
        });

    return newItem;
  }

  public boolean updateItemFull(EditProductRequestDTO requestPayload) {
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

  public List<Item> getAllItem(PendingItemsDTO requestPayload) throws Exception {
    int sellerId = requestPayload.getSellerId();
    if (sellerId <= 0) {
      throw new Exception("Invalid seller ID.");
    }
    List<Item> items = itemDAO.getAllItemByUserId(sellerId);
    return items;
  }

  public List<Item> getAllItemByStatus(ItemStatus status) throws Exception {
    List<Item> items = itemDAO.getItemsByStatus(status);
    return items;
  }

  public List<Item> getApprovedItemsByUserId(int userId) {
    return itemDAO.getApprovedItemsByUserId(userId);
  }

  public boolean deleteItem(DeleteRequestDTO requestPayload) throws Exception {
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

  public Item getItemById(int itemId) throws Exception {
    if (itemId <= 0) {
      throw new Exception("Invalid item ID.");
    }
    Item item = itemDAO.getItemById(itemId);
    if (item == null) {
      throw new Exception("Item not found.");
    }
    return item;
  }

  public boolean updateItemStatus(int itemId, ItemStatus newStatus) throws Exception {
    if (itemId <= 0) {
      throw new Exception("Invalid item ID.");
    }
    if (newStatus == null) {
      throw new Exception("New status is required.");
    }
    boolean success = itemDAO.updateItemStatus(itemId, newStatus);
    if (!success) {
      throw new Exception("Cannot update item status.");
    }
    return true;
  }

  private void validateItemData(CreateItemRequestDTO item) throws Exception {
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
