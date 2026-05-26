package org.example.server.services;

import org.example.core.dto.admin.AiEvaluationDTO;
import org.example.core.dto.itemsDTO.CreateItemRequestDTO;
import org.example.core.dto.itemsDTO.DeleteRequestDTO;
import org.example.core.dto.itemsDTO.EditProductRequestDTO;
import org.example.core.dto.itemsDTO.PendingItemsDTO;
import org.example.core.exception.DataConflictException;
import org.example.core.exception.DatabaseAccessException;
import org.example.core.exception.InvalidUserDataException;
import org.example.core.exception.ResourceNotFoundException;
import org.example.core.models.items.Item;
import org.example.core.models.items.ItemFactory;
import org.example.core.shared.enums.ItemStatus;
import org.example.server.daos.ItemDAO;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dịch vụ quản lý kho đồ tài sản.
 */
public class ItemService {
  private static final Logger logger = Logger.getLogger(ItemService.class.getName());
  private static volatile ItemService instance = null;
  private final ItemDAO itemDAO;

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

  public Item createItem(CreateItemRequestDTO requestPayload) {
    validateItemData(requestPayload);

    Item newItem = ItemFactory.createItemDTO(requestPayload);
    if (newItem == null) {
      throw new InvalidUserDataException("Lỗi khởi tạo: Nhà máy cấu trúc không thể đóng gói sản phẩm mới!");
    }

    Integer itemId = itemDAO.insertIntoItemTable(newItem);
    newItem.setItemId(itemId);
    boolean isSuccess = itemDAO.insertIntoChildTable(newItem, itemId);
    if(!isSuccess) {
      throw new DatabaseAccessException("Không thể lưu trữ thông tin chi tiết của vật phẩm vào cơ sở dữ liệu!");
    }

    CompletableFuture.runAsync(() -> {
      try {
        logger.info("🤖 AI bắt đầu quét thẩm định vật phẩm ID: " + itemId);
        AiEvaluationDTO evaluation = AiModerationService.evaluateItem(newItem);

        newItem.setSuggestedPrice(evaluation.getSuggestedPrice());
        newItem.setAiReason(evaluation.getReason());

        if (evaluation.isSafe()) {
          newItem.setStatus(ItemStatus.APPROVED);
          logger.info("✅ AI tự động APPROVED cho vật phẩm ID: " + itemId);
        } else {
          logger.log(Level.WARNING, "AI phát hiện rủi ro tại ID: " + itemId);
        }
        itemDAO.updateAiEvaluation(newItem);
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Lỗi tiến trình AI kiểm duyệt ngầm", e);
      }
    });

    return newItem;
  }

  public boolean updateItemFull(EditProductRequestDTO requestPayload) {
    if (requestPayload == null) {
      throw new InvalidUserDataException("Dữ liệu chỉnh sửa thông tin rỗng!");
    }
    int itemId = requestPayload.getItemId();
    String newName = requestPayload.getItemEditName();
    String newDescription = requestPayload.getDescription();
    BigDecimal newPrice = requestPayload.getPrice();

    validateEditFields(itemId, newName, newDescription, newPrice);

    Item currentItem = itemDAO.getItemById(itemId);
    if (currentItem.getStatus() == ItemStatus.LISTED) {
      throw new DataConflictException("Sản phẩm đang trong phiên đấu giá trực tuyến, nghiêm cấm chỉnh sửa!");
    }

    boolean isSuccess = itemDAO.updateItemDescriptionByItemId(itemId, newDescription)
            && itemDAO.updateItemNameByItemId(itemId, newName)
            && itemDAO.updateStartPriceByItemId(itemId, newPrice);
    if(!isSuccess) {
      throw new DatabaseAccessException("Không thể cập nhật vật phẩm!");
    }
    return isSuccess;
  }

  public boolean deleteItem(DeleteRequestDTO requestPayload) {
    if (requestPayload == null) {
      throw new InvalidUserDataException("Yêu cầu loại bỏ sản phẩm không hợp lệ.");
    }
    int itemId = requestPayload.getItemId();
    if (itemId <= 0) {
      throw new InvalidUserDataException("Mã vật phẩm yêu cầu xóa bỏ không hợp lệ.");
    }

    Item currentItem = itemDAO.getItemById(itemId);
    if (currentItem.getStatus() == ItemStatus.LISTED) {
      throw new DataConflictException("Không thể xóa vật phẩm này do nó đang được mở đấu giá công khai!");
    }

    return itemDAO.deleteItemByItemId(itemId);
  }

  public boolean updateItemStatus(int itemId, ItemStatus newStatus) {
    if (itemId <= 0) {
      throw new InvalidUserDataException("Mã số nhận diện vật phẩm không hợp lệ.");
    }
    if (newStatus == null) {
      throw new InvalidUserDataException("Trạng thái cập nhật mới không được để trống.");
    }
    return itemDAO.updateItemStatus(itemId, newStatus);
  }

  public List<Item> getAllItem(PendingItemsDTO requestPayload) {
    if (requestPayload == null) {
      throw new InvalidUserDataException("Thông tin yêu cầu lọc danh mục sản phẩm trống!");
    }
    int sellerId = requestPayload.getSellerId();
    if (sellerId <= 0) {
      throw new InvalidUserDataException("Mã định danh người bán (Seller ID) không hợp lệ.");
    }
    return itemDAO.getAllItemByUserId(sellerId);
  }

  public List<Item> getAllItemByStatus(ItemStatus status) {
    if (status == null) {
      throw new InvalidUserDataException("Trạng thái kiểm duyệt cần lọc bắt buộc phải chọn.");
    }
    return itemDAO.getItemsByStatus(status);
  }

  public List<Item> getApprovedItemsByUserId(int userId) {
    if (userId <= 0) {
      throw new InvalidUserDataException("Mã người dùng không hợp lệ để tra cứu kho đồ APPROVED!");
    }
    return itemDAO.getApprovedItemsByUserId(userId);
  }

  public Item getItemById(int itemId) {
    if (itemId <= 0) {
      throw new InvalidUserDataException("Mã số nhận diện vật phẩm không hợp lệ.");
    }
    return itemDAO.getItemById(itemId);
  }

  private void validateItemData(CreateItemRequestDTO item) {
    if (item == null) throw new InvalidUserDataException("Dữ liệu khai báo thông tin vật phẩm bắt buộc phải có.");
    if (item.getSellerID() <= 0) throw new InvalidUserDataException("Định danh tài khoản người bán không hợp lệ.");
    if (item.getType() == null || item.getType().trim().isEmpty()) throw new InvalidUserDataException("Vui lòng phân loại danh mục nhóm (Type)!");
    if (item.getItemName() == null || item.getItemName().trim().isEmpty()) throw new InvalidUserDataException("Tên gọi hiển thị không được phép để trống!");
    if (item.getDescription() == null || item.getDescription().trim().isEmpty()) throw new InvalidUserDataException("Vui lòng điền văn bản mô tả tình trạng vật phẩm!");
    if (item.getStartingPrice() == null || item.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) throw new InvalidUserDataException("Giá sàn khởi điểm buộc phải lớn hơn 0 VNĐ!");
    if (item.getBase64Image() == null || item.getBase64Image().trim().isEmpty()) throw new InvalidUserDataException("Vui lòng đăng tải hình ảnh minh họa!");
  }

  private void validateEditFields(int id, String name, String desc, BigDecimal price) {
    if (id <= 0) throw new InvalidUserDataException("Mã vật phẩm chỉnh sửa không hợp lệ!");
    if (name == null || name.trim().isEmpty()) throw new InvalidUserDataException("Tên sản phẩm cập nhật mới không được phép bỏ trống!");
    if (desc == null || desc.trim().isEmpty()) throw new InvalidUserDataException("Nội dung mô tả sản phẩm mới không được để trống!");
    if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) throw new InvalidUserDataException("Mức giá khởi điểm mới phải lớn hơn 0 VNĐ!");
  }
}