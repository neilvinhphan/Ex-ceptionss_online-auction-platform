package org.example.server.services;

import org.example.core.dto.auctionDTO.CreateAuctionDTO;
import org.example.core.models.entities.Auction;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.Item;
import org.example.core.models.users.User;
import org.example.core.shared.enums.AuctionStatus;
import org.example.core.shared.enums.ItemStatus;
import org.example.core.shared.enums.WalletTransactionType;
import org.example.server.daos.AuctionDAO;
import org.example.server.daos.ItemDAO;
import org.example.server.daos.UserDAO;
import org.example.server.daos.WalletDAO;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// 🛠️ Kích hoạt tính năng sắp xếp thứ tự chạy theo Annotation @Order
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionServiceTest {

    private AuctionDAO auctionDAOMock;
    private UserDAO userDAOMock;
    private ItemDAO itemDAOMock;
    private WalletDAO walletDAOMock;

    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        // Mỗi test case đều nhận một tập hợp Mock hoàn toàn mới, triệt tiêu 100% xung đột dữ liệu ngầm
        auctionDAOMock = mock(AuctionDAO.class);
        userDAOMock = mock(UserDAO.class);
        itemDAOMock = mock(ItemDAO.class);
        walletDAOMock = mock(WalletDAO.class);

        auctionService = new AuctionService(auctionDAOMock, userDAOMock, itemDAOMock, walletDAOMock);
    }

    // =========================================================================
    // NHÓM 1: KIỂM THỬ LUỒNG TẠO PHÒNG ĐẤU GIÁ (CREATE AUCTION)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("1. Tạo phòng thất bại do Item bị Null")
    void testCreateAuction_ItemNull_ThrowsException() {
        CreateAuctionDTO dto = new CreateAuctionDTO();
        dto.setItem(null);

        Exception exception = assertThrows(Exception.class, () -> auctionService.createAuction(dto));
        assertEquals("Vật phẩm không tồn tại!", exception.getMessage());
    }

    @Test
    @Order(2)
    @DisplayName("2. Tạo phòng thất bại do Vật phẩm đang được đấu giá ở phòng khác")
    void testCreateAuction_ItemAlreadyListed_ThrowsException() {
        Item mockItem = new ArtItem();
        mockItem.setStatus(ItemStatus.LISTED);

        CreateAuctionDTO dto = new CreateAuctionDTO();
        dto.setItem(mockItem);

        Exception exception = assertThrows(Exception.class, () -> auctionService.createAuction(dto));
        assertEquals("Vật phẩm đang được đấu giá!", exception.getMessage());
    }

    @Test
    @Order(3)
    @DisplayName("3. Tạo phòng Đấu giá thành công")
    void testCreateAuction_ValidData_Success() throws Exception {
    Item mockItem = new ArtItem();
        mockItem.setStatus(ItemStatus.APPROVED);

        CreateAuctionDTO dto = new CreateAuctionDTO();
        dto.setItem(mockItem);
        dto.setDurationMinutes(30);
        dto.setBidIncrement(new BigDecimal("10000"));
        dto.setStartTime(LocalDateTime.now().plusMinutes(5));

        Auction expectedAuction = new Auction();
        expectedAuction.setAuctionId(99);
        expectedAuction.setStartTime(LocalDateTime.now().plusMinutes(5));

        when(auctionDAOMock.createNewAuctionItem(any(), anyLong(), any(), any())).thenReturn(99);
        when(auctionDAOMock.getAuctionByAuctionId(99)).thenReturn(expectedAuction);

        Auction result = auctionService.createAuction(dto);

        assertNotNull(result);
        assertEquals(99, result.getAuctionId());
        verify(auctionDAOMock, times(1)).createNewAuctionItem(mockItem, 30, new BigDecimal("10000"), dto.getStartTime());
    }

    // =========================================================================
    // NHÓM 2: KIỂM THỬ LUỒNG THANH TOÁN (CHECKOUT AUCTION)
    // =========================================================================

    private final int auctionId = 1;
    private final int winnerId = 10;
    private final int sellerId = 20;
    private final int itemId = 50;
    private final BigDecimal highestBid = new BigDecimal("500000");

    private void prepareMockForCheckout(AuctionStatus status, int bidderId, BigDecimal walletBalance) throws Exception {
        Auction mockAuction = new Auction();
        mockAuction.setAuctionId(auctionId);
        mockAuction.setItemId(itemId);
        mockAuction.setBidderId(bidderId); // Gán người thắng cuộc
        mockAuction.setHighestBid(highestBid);
        mockAuction.setStatus(status);

        User mockWinner = new User();
        mockWinner.setUserId(winnerId);
        mockWinner.setBalance(new BigDecimal("1000000"));

        User mockSeller = new User();
        mockSeller.setUserId(sellerId);
        mockSeller.setBalance(new BigDecimal("200000"));

        when(auctionDAOMock.getAuctionByAuctionId(auctionId)).thenReturn(mockAuction);
        when(itemDAOMock.getOwnerIdByItemId(itemId)).thenReturn(sellerId);
        when(userDAOMock.getUserByUserId(winnerId)).thenReturn(mockWinner);
        when(userDAOMock.getUserByUserId(sellerId)).thenReturn(mockSeller);
        when(walletDAOMock.getAvailableBalance(winnerId)).thenReturn(walletBalance);
    }

    @Test
    @Order(4)
    @DisplayName("4. Thanh toán thất bại do Phòng đã hoàn tất thanh toán trước đó")
    void testCheckout_AlreadyPaid_ThrowsException() throws Exception {
        // Giả lập phòng đã ở trạng thái PAID
        prepareMockForCheckout(AuctionStatus.PAID, winnerId, new BigDecimal("1000000"));

        Exception exception = assertThrows(Exception.class, () -> auctionService.checkoutAuction(auctionId, winnerId));
        assertEquals("Phiên đấu giá đã được thanh toán trước đó!", exception.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("5. Thanh toán thất bại do Sai ID người thắng cuộc")
    void testCheckout_WrongWinner_ThrowsException() throws Exception {
        // Người thắng thực tế trong DB là winnerId (10), nhưng kẻ yêu cầu là 999
        prepareMockForCheckout(AuctionStatus.FINISHED, winnerId, new BigDecimal("1000000"));
        int wrongWinnerId = 999;

        Exception exception = assertThrows(Exception.class, () -> auctionService.checkoutAuction(auctionId, wrongWinnerId));
        assertEquals("Bạn không phải người thắng phiên đấu giá này!", exception.getMessage());
    }

    @Test
    @Order(6)
    @DisplayName("6. Thanh toán thất bại do Số dư tài khoản ví không đủ")
    void testCheckout_InsufficientBalance_ThrowsException() throws Exception {
        // Số dư khả dụng trong ví chỉ có 10,000, không đủ trả giá 500,000
        prepareMockForCheckout(AuctionStatus.FINISHED, winnerId, new BigDecimal("10000"));

        Exception exception = assertThrows(Exception.class, () -> auctionService.checkoutAuction(auctionId, winnerId));
        assertEquals("Số dư tài khoản không đủ để thanh toán!", exception.getMessage());
    }

    @Test
    @Order(7)
    @DisplayName("7. Thanh toán thành công (Luồng chuẩn)")
    void testCheckout_Success() throws Exception {
        // Tài khoản đủ 1,000,000 để chi trả cho vật phẩm 500,000
        prepareMockForCheckout(AuctionStatus.FINISHED, winnerId, new BigDecimal("1000000"));

        boolean result = auctionService.checkoutAuction(auctionId, winnerId);

        assertTrue(result);

        // Khảo sát sự thay đổi tài khoản
        verify(userDAOMock, times(1)).updateBalanceInDB(winnerId, new BigDecimal("500000"));
        verify(userDAOMock, times(1)).updateBalanceInDB(sellerId, new BigDecimal("700000"));

        // Khảo sát việc tạo lịch sử giao dịch
        verify(walletDAOMock, times(1)).insertWalletTransaction(winnerId, highestBid, WalletTransactionType.PAY_AUCTION, auctionId);
        verify(walletDAOMock, times(1)).insertWalletTransaction(sellerId, highestBid, WalletTransactionType.SELL_REVENUE, auctionId);

        // Khảo sát cập nhật thực thể
        verify(auctionDAOMock, times(1)).setAuctionStatus(auctionId, AuctionStatus.PAID);
        verify(itemDAOMock, times(1)).updateOwnerIdByItemId(itemId, winnerId);
    }
}