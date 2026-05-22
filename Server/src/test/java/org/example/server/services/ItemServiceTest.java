package org.example.server.services;

import org.example.core.dto.itemsDTO.CreateItemRequestDTO;
import org.example.core.dto.itemsDTO.CreateVehicleItemDTO;
import org.example.core.dto.itemsDTO.DeleteRequestDTO;
import org.example.core.dto.itemsDTO.EditProductRequestDTO;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;
import org.example.core.shared.enums.ItemStatus;
import org.example.server.daos.ItemDAO;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// 🛠️ Kích hoạt tính năng sắp xếp thứ tự chạy nghiêm ngặt để kiểm soát luồng test
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItemServiceTest {

    private ItemDAO itemDAOMock;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        // Khởi tạo mới Mock độc lập trước mỗi hàm test để triệt tiêu 100% xung đột dữ liệu ngầm
        itemDAOMock = mock(ItemDAO.class);
        itemService = new ItemService(itemDAOMock);
    }

    // =========================================================================
    // NHÓM 1: KIỂM THỬ LUỒNG VALIDATE VÀ TẠO VẬT PHẨM (createItem)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("1. Tạo vật phẩm thất bại do dữ liệu Payload đầu vào bị null")
    void testCreateItem_PayloadNull_ThrowsException() {
        Exception exception = assertThrows(Exception.class, () -> itemService.createItem(null));
        assertEquals("Item data is required.", exception.getMessage());
    }

    @Test
    @Order(2)
    @DisplayName("2. Tạo vật phẩm thất bại do tên vật phẩm trống")
    void testCreateItem_EmptyItemName_ThrowsException() {
        CreateItemRequestDTO dto = new CreateVehicleItemDTO();
        dto.setSellerID(1);
        dto.setType("VEHICLE");
        dto.setItemName(""); // Cố tình để trống tên
        dto.setDescription("Xe máy chính chủ ít sử dụng");
        dto.setStartingPrice(new BigDecimal("15000000"));
        dto.setBase64Image("data:image/png;base64,...");

        Exception exception = assertThrows(Exception.class, () -> itemService.createItem(dto));
        assertEquals("Item name is required.", exception.getMessage());
    }

    @Test
    @Order(3)
    @DisplayName("3. Tạo vật phẩm thất bại do giá khởi điểm nhỏ hơn hoặc bằng 0")
    void testCreateItem_InvalidStartingPrice_ThrowsException() {
        CreateItemRequestDTO dto = new CreateVehicleItemDTO();
        dto.setSellerID(1);
        dto.setType("VEHICLE");
        dto.setItemName("Xe máy Honda Vision");
        dto.setDescription("Xe máy chính chủ");
        dto.setStartingPrice(new BigDecimal("-500000")); // Giá khởi điểm bị âm
        dto.setBase64Image("data:image/png;base64,...");

        Exception exception = assertThrows(Exception.class, () -> itemService.createItem(dto));
        assertEquals("Starting price must be greater than zero.", exception.getMessage());
    }

    @Test
    @Order(4)
    @DisplayName("4. Tạo vật phẩm thất bại do lỗi không ghi nhận được vào Database")
    void testCreateItem_DatabaseInsertFailure_ThrowsException() throws Exception {
        CreateItemRequestDTO dto = new CreateVehicleItemDTO();
        dto.setSellerID(1);
        dto.setType("VEHICLE");
        dto.setItemName("Xe máy Honda Vision");
        dto.setDescription("Xe máy chính chủ");
        dto.setStartingPrice(new BigDecimal("15000000"));
        dto.setBase64Image("data:image/png;base64,...");

        // Giả lập bảng Item của DB bị lỗi, trả ra ID không hợp lệ (null)
        when(itemDAOMock.insertIntoItemTable(any(Item.class))).thenReturn(0);

        Exception exception = assertThrows(Exception.class, () -> itemService.createItem(dto));
        assertEquals("Cannot create item.", exception.getMessage());
    }

    // =========================================================================
    // NHÓM 2: KIỂM THỬ LUỒNG CẬP NHẬT VÀ XÓA SẢN PHẨM
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("5. Cập nhật toàn bộ thông tin sản phẩm thành công")
    void testUpdateItemFull_Success() {
        EditProductRequestDTO dto = new EditProductRequestDTO();
        dto.setItemId(100);
        dto.setItemEditName("Honda Vision 2013 Đã Độ");
        dto.setDescription("Xe máy cũ chạy êm, đã bảo dưỡng bộ nồi");
        dto.setPrice(new BigDecimal("12500000"));

        // Cấu hình mock cho cả 3 hàm update thành phần trong DAO đều hoạt động trơn tru
        when(itemDAOMock.updateItemDescriptionByItemId(100, "Xe máy cũ chạy êm, đã bảo dưỡng bộ nồi")).thenReturn(true);
        when(itemDAOMock.updateItemNameByItemId(100, "Honda Vision 2013 Đã Độ")).thenReturn(true);
        when(itemDAOMock.updateStartPriceByItemId(100, new BigDecimal("12500000"))).thenReturn(true);

        boolean result = itemService.updateItemFull(dto);

        assertTrue(result);
        verify(itemDAOMock, times(1)).updateItemDescriptionByItemId(100, "Xe máy cũ chạy êm, đã bảo dưỡng bộ nồi");
        verify(itemDAOMock, times(1)).updateItemNameByItemId(100, "Honda Vision 2013 Đã Độ");
        verify(itemDAOMock, times(1)).updateStartPriceByItemId(100, new BigDecimal("12500000"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Cập nhật thông tin sản phẩm thất bại do lỗi ghi nhận một bảng thành phần")
    void testUpdateItemFull_Failure() {
        EditProductRequestDTO dto = new EditProductRequestDTO();
        dto.setItemId(100);
        dto.setItemEditName("Sản phẩm lỗi");
        dto.setDescription("Mô tả lỗi");
        dto.setPrice(new BigDecimal("100000"));

        // Giả lập hàm cập nhật mô tả bị thất bại (trả về false) khiến toàn bộ tiến trình báo lỗi theo
        when(itemDAOMock.updateItemDescriptionByItemId(anyInt(), anyString())).thenReturn(false);

        boolean result = itemService.updateItemFull(dto);

        assertFalse(result);
    }

    @Test
    @Order(7)
    @DisplayName("7. Xóa vật phẩm thất bại do ID truyền vào không hợp lệ (<= 0)")
    void testDeleteItem_InvalidId_ThrowsException() {
        DeleteRequestDTO dto = new DeleteRequestDTO(0);

        Exception exception = assertThrows(Exception.class, () -> itemService.deleteItem(dto));
        assertEquals("Invalid item ID.", exception.getMessage());
    }

    @Test
    @Order(8)
    @DisplayName("8. Xóa vật phẩm thành công khỏi hệ thống Database")
    void testDeleteItem_Success() throws Exception {
        DeleteRequestDTO dto = new DeleteRequestDTO(50);

        when(itemDAOMock.deleteItemByItemId(50)).thenReturn(true);

        boolean result = itemService.deleteItem(dto);

        assertTrue(result);
        verify(itemDAOMock, times(1)).deleteItemByItemId(50);
    }

    // =========================================================================
    // NHÓM 3: KIỂM THỬ LUỒNG TRUY VẤN DỮ LIỆU VÀ ĐỔI TRẠNG THÁI (STATUS)
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("9. Tìm kiếm sản phẩm theo ID thành công (Trả về đúng thực thể loại Vehicle)")
    void testGetItemById_Success() throws Exception {
    // Dựng một thực thể con thực tế để khớp với ItemFactory của bạn
        Item expectedItem = new VehicleItem();
        expectedItem.setItemId(10);
        expectedItem.setItemName("Xe máy Honda Vision 2013");

        when(itemDAOMock.getItemById(10)).thenReturn(expectedItem);

        Item result = itemService.getItemById(10);

        assertNotNull(result);
        assertEquals(10, result.getItemId());
        assertEquals("Xe máy Honda Vision 2013", result.getItemName());
        assertTrue(result instanceof VehicleItem, "Thực thể trả về phải là một instance của lớp Vehicle");
    }

    @Test
    @Order(10)
    @DisplayName("10. Cập nhật trạng thái vật phẩm thành công")
    void testUpdateItemStatus_Success() throws Exception {
        when(itemDAOMock.updateItemStatus(10, ItemStatus.APPROVED)).thenReturn(true);

        boolean result = itemService.updateItemStatus(10, ItemStatus.APPROVED);

        assertTrue(result);
        verify(itemDAOMock, times(1)).updateItemStatus(10, ItemStatus.APPROVED);
    }
}