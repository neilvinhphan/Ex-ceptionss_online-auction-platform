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
    if (newItem == null) {
      throw new Exception("Lỗi khởi tạo: Nhà máy cấu trúc dữ liệu không thể đóng gói sản phẩm mới này!");
    }

    Integer itemId = itemDAO.insertIntoItemTable(newItem);
    if (itemId == null || itemId <= 0) {
      throw new Exception("Ghi nhận thất bại: Hệ thống cơ sở dữ liệu từ chối cấp phép lưu vật phẩm mới.");
    }

    newItem.setItemId(itemId);
    boolean insertedChild = itemDAO.insertIntoChildTable(newItem, itemId);
    if (!insertedChild) {
      throw new Exception("Lỗi hệ thống: Không thể đồng bộ thuộc tính mở rộng chi tiết của vật phẩm vào bảng con.");
    }

    // 2. 🚀 CHẠY NGẦM (ASYNC): Gọi AI để thẩm định nội dung và định giá
    CompletableFuture.runAsync(
            () -> {
              try {
                System.out.println("🤖 AI đang bắt đầu quét vật phẩm ID: " + itemId);

                AiEvaluationDTO evaluation = AiModerationService.evaluateItem(newItem);

                newItem.setSuggestedPrice(evaluation.getSuggestedPrice());
                newItem.setAiReason(evaluation.getReason());

                if (evaluation.isSafe()) {
                  newItem.setStatus(ItemStatus.APPROVED);
                  System.out.println("✅ AI tự động DUYỆT vật phẩm ID: " + itemId);
                } else {
                  System.out.println("🚩 AI phát hiện nghi vấn tại ID: " + itemId + ". Giữ trạng thái PENDING cho Admin.");
                }

                itemDAO.updateAiEvaluation(newItem);

              } catch (Exception e) {
                System.err.println("❌ Lỗi trong quá trình AI thẩm định: " + e.getMessage());
              }
            });

    return newItem;
  }

  public boolean updateItemFull(EditProductRequestDTO requestPayload) throws Exception {
    if (requestPayload == null) {
      throw new Exception("Dữ liệu chỉnh sửa cập nhật thông tin trống!");
    }
    int itemId = requestPayload.getItemId();
    String newName = requestPayload.getItemEditName();
    String newDescription = requestPayload.getDescription();
    BigDecimal newPrice = requestPayload.getPrice();

    if (itemId <= 0) {
      throw new Exception("Mã vật phẩm chỉnh sửa không hợp lệ!");
    }
    if (newName == null || newName.trim().isEmpty()) {
      throw new Exception("Tên sản phẩm cập nhật mới không được phép bỏ trống!");
    }
    if (newDescription == null || newDescription.trim().isEmpty()) {
      throw new Exception("Nội dung mô tả sản phẩm mới không được để trống!");
    }
    if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
      throw new Exception("Mức giá khởi điểm mới thay đổi phải lớn hơn 0 VNĐ!");
    }

    Item currentItem = itemDAO.getItemById(itemId);
    if (currentItem == null) {
      throw new Exception("Sản phẩm cần cập nhật không tồn tại hoặc đã bị xóa trước đó!");
    }
    if (currentItem.getStatus() == ItemStatus.LISTED) {
      throw new Exception("Sản phẩm này đang trong phiên đấu giá trực tuyến, nghiêm cấm chỉnh sửa thông tin!");
    }

    if (itemDAO.updateItemDescriptionByItemId(itemId, newDescription)
            && itemDAO.updateItemNameByItemId(itemId, newName)
            && itemDAO.updateStartPriceByItemId(itemId, newPrice)) {
      return true;
    } else {
      throw new Exception("Lỗi hệ thống: Quá trình cập nhật thuộc tính vật phẩm vào Database thất bại.");
    }
  }

  public List<Item> getAllItem(PendingItemsDTO requestPayload) throws Exception {
    if (requestPayload == null) {
      throw new Exception("Thông tin yêu cầu lọc danh mục sản phẩm của đối tác trống!");
    }
    int sellerId = requestPayload.getSellerId();
    if (sellerId <= 0) {
      throw new Exception("Mã định danh người bán (Seller ID) không hợp lệ.");
    }
    List<Item> items = itemDAO.getAllItemByUserId(sellerId);
    if (items == null) {
      throw new Exception("Lỗi truy xuất: Không thể tải danh sách sản phẩm từ cơ sở dữ liệu.");
    }
    return items;
  }

  public List<Item> getAllItemByStatus(ItemStatus status) throws Exception {
    if (status == null) {
      throw new Exception("Trạng thái kiểm duyệt cần lọc tìm kiếm bắt buộc phải chọn.");
    }
    List<Item> items = itemDAO.getItemsByStatus(status);
    return items;
  }

  public List<Item> getApprovedItemsByUserId(int userId) throws Exception {
    if (userId <= 0) {
      throw new Exception("Mã người dùng không hợp lệ để tra cứu kho đồ đã phê duyệt!");
    }
    return itemDAO.getApprovedItemsByUserId(userId);
  }

  public boolean deleteItem(DeleteRequestDTO requestPayload) throws Exception {
    if (requestPayload == null) {
      throw new Exception("Yêu cầu loại bỏ sản phẩm không hợp lệ.");
    }
    int itemId = requestPayload.getItemId();
    if (itemId <= 0) {
      throw new Exception("Mã vật phẩm yêu cầu xóa bỏ không hợp lệ.");
    }

    Item currentItem = itemDAO.getItemById(itemId);
    if (currentItem == null) {
      throw new Exception("Sản phẩm không tồn tại trên hệ thống để thực hiện lệnh xóa.");
    }
    if (currentItem.getStatus() == ItemStatus.LISTED) {
      throw new Exception("Không thể xóa vật phẩm này do nó đang được mở đấu giá công khai!");
    }

    boolean success = itemDAO.deleteItemByItemId(itemId);
    if (!success) {
      throw new Exception("Lỗi hệ thống: Cơ sở dữ liệu từ chối thực thi lệnh xóa bỏ vật phẩm.");
    }
    return true;
  }

  public Item getItemById(int itemId) throws Exception {
    if (itemId <= 0) {
      throw new Exception("Mã số nhận diện vật phẩm (Item ID) không hợp lệ.");
    }
    Item item = itemDAO.getItemById(itemId);
    if (item == null) {
      throw new Exception("Rất tiếc, hệ thống không tìm thấy vật phẩm có mã ID: " + itemId);
    }
    return item;
  }

  public boolean updateItemStatus(int itemId, ItemStatus newStatus) throws Exception {
    if (itemId <= 0) {
      throw new Exception("Mã số nhận diện vật phẩm không hợp lệ để chuyển trạng thái.");
    }
    if (newStatus == null) {
      throw new Exception("Trạng thái cập nhật mới yêu cầu bắt buộc và không được để trống.");
    }
    boolean success = itemDAO.updateItemStatus(itemId, newStatus);
    if (!success) {
      throw new Exception("Lỗi hệ thống: Tiến trình cập nhật lại trạng thái phê duyệt của vật phẩm thất bại.");
    }
    return true;
  }

  private void validateItemData(CreateItemRequestDTO item) throws Exception {
    if (item == null) {
      throw new Exception("Dữ liệu khai báo thông tin vật phẩm bắt buộc phải có.");
    }
    if (item.getSellerID() <= 0) {
      throw new Exception("Định danh tài khoản người bán tạo sản phẩm không hợp lệ.");
    }
    if (item.getType() == null || item.getType().trim().isEmpty()) {
      throw new Exception("Vui lòng phân loại danh mục nhóm (Type) cho sản phẩm đấu giá!");
    }
    if (item.getItemName() == null || item.getItemName().trim().isEmpty()) {
      throw new Exception("Tên gọi hiển thị của vật phẩm không được phép để trống!");
    }
    if (item.getDescription() == null || item.getDescription().trim().isEmpty()) {
      throw new Exception("Vui lòng điền văn bản mô tả thông số/chi tiết về tình trạng vật phẩm!");
    }
    if (item.getStartingPrice() == null
            || item.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new Exception("Số tiền giá sàn khởi điểm đấu giá (Starting Price) buộc phải lớn hơn 0 VNĐ!");
    }
    if (item.getBase64Image() == null || item.getBase64Image().trim().isEmpty()) {
      throw new Exception("Vui lòng đăng tải hình ảnh minh họa đính kèm trực quan cho vật phẩm!");
    }
  }
}