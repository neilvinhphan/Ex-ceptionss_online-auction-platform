package org.example.server.services;

import org.example.core.dto.bidDTO.BidRequestDTO;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.models.users.User;
import org.example.core.shared.enums.AuctionStatus;
import org.example.server.daos.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BiddingServiceTest {

    private BidDAO bidDAOMock;
    private AuctionDAO auctionDAOMock;
    private WalletDAO walletDAOMock;
    private UserDAO userDAOMock;
    private AutoBidDAO autoBidDAOMock;

    private BiddingService biddingService;

    @BeforeEach
    void setUp() {
        // Khởi tạo mới Mock độc lập trước mỗi phương thức test để tránh xung đột dữ liệu ngầm
        bidDAOMock = mock(BidDAO.class);
        auctionDAOMock = mock(AuctionDAO.class);
        walletDAOMock = mock(WalletDAO.class);
        userDAOMock = mock(UserDAO.class);
        autoBidDAOMock = mock(AutoBidDAO.class);

        biddingService = new BiddingService(bidDAOMock, auctionDAOMock, walletDAOMock, userDAOMock, autoBidDAOMock);
    }

    // =========================================================================
    // NHÓM 1: KIỂM THỬ LUỒNG ĐẶT GIÁ BẰNG TAY (placeBid)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("1. Đặt giá thất bại do không tìm thấy phiên đấu giá")
    void testPlaceBid_AuctionNotFound_ThrowsException() throws Exception {
        BidRequestDTO request = new BidRequestDTO(1, 10, new BigDecimal("150000"), "HoangThong");
        when(auctionDAOMock.getAuctionByAuctionId(1)).thenReturn(null);

        Exception exception = assertThrows(Exception.class, () -> biddingService.placeBid(request));
        assertEquals("Không tìm thấy phiên đấu giá.", exception.getMessage());
    }

    @Test
    @Order(2)
    @DisplayName("2. Đặt giá thất bại do số tiền đặt nhỏ hơn mức chấp nhận tối thiểu")
    void testPlaceBid_BidAmountLessThanMinAcceptable_ThrowsException() throws Exception {
        BidRequestDTO request = new BidRequestDTO(1, 10, new BigDecimal("120000"), "HoangThong"); // Đặt 120k

        Auction mockAuction = mock(Auction.class);
        when(auctionDAOMock.getAuctionByAuctionId(1)).thenReturn(mockAuction);

        // Giá hiện tại là 100k, bước giá là 30k -> Mức chấp nhận tối thiểu phải là 130k
        when(bidDAOMock.getCurrentPrice(1)).thenReturn(new BigDecimal("100000"));
        when(auctionDAOMock.getBidIncrementByAuctionId(1)).thenReturn(new BigDecimal("30000"));

        Exception exception = assertThrows(Exception.class, () -> biddingService.placeBid(request));
        assertTrue(exception.getMessage().contains("Giá đặt phải >="));
    }

    @Test
    @Order(3)
    @DisplayName("3. Đặt giá thất bại do số dư ví khả dụng không đủ")
    void testPlaceBid_InsufficientWalletBalance_ThrowsException() throws Exception {
        BidRequestDTO request = new BidRequestDTO(1, 10, new BigDecimal("200000"), "HoangThong");

        Auction mockAuction = mock(Auction.class);
        when(auctionDAOMock.getAuctionByAuctionId(1)).thenReturn(mockAuction);
        when(bidDAOMock.getCurrentPrice(1)).thenReturn(new BigDecimal("100000"));
        when(auctionDAOMock.getBidIncrementByAuctionId(1)).thenReturn(new BigDecimal("10000"));

        // Số dư ví chỉ còn 50k, không đủ trả mức giá 200k
        when(walletDAOMock.getAvailableBalance(10)).thenReturn(new BigDecimal("50000"));

        Exception exception = assertThrows(Exception.class, () -> biddingService.placeBid(request));
        assertEquals("Sô dư khả dụng không đủ!", exception.getMessage());
    }

    @Test
    @Order(4)
    @DisplayName("4. Đặt giá bằng tay thành công (Luồng chuẩn, kích hoạt Anti-Sniping)")
    void testPlaceBid_Success() throws Exception {
        BidRequestDTO request = new BidRequestDTO(1, 10, new BigDecimal("150000"), "HoangThong");

        Auction mockAuction = new Auction();
        mockAuction.setAuctionId(1);
        mockAuction.setStatus(AuctionStatus.RUNNING);
        mockAuction.setStartTime(LocalDateTime.now().minusMinutes(5));
        mockAuction.setEndTime(LocalDateTime.now().plusSeconds(30)); // Đang nằm trong khung giờ Sniping (30s < 120s)

        User mockUser = new User();
        mockUser.setUserName("HoangThong");

        when(auctionDAOMock.getAuctionByAuctionId(1)).thenReturn(mockAuction);
        when(bidDAOMock.getCurrentPrice(1)).thenReturn(new BigDecimal("100000"));
        when(auctionDAOMock.getBidIncrementByAuctionId(1)).thenReturn(new BigDecimal("10000"));
        when(walletDAOMock.getAvailableBalance(10)).thenReturn(new BigDecimal("500000"));
        when(userDAOMock.getUserByUserId(10)).thenReturn(mockUser);

        when(bidDAOMock.insertBid(any(BidTransaction.class))).thenReturn(true);
        when(bidDAOMock.updateCurrentPrice(eq(1), eq(10), any(BigDecimal.class))).thenReturn(true);

        boolean result = biddingService.placeBid(request);

        assertTrue(result);
        verify(bidDAOMock, times(1)).insertBid(any(BidTransaction.class));
        verify(bidDAOMock, times(1)).updateCurrentPrice(1, 10, new BigDecimal("150000"));
        verify(auctionDAOMock, times(1)).updateAuctionEndTime(eq(1), any(LocalDateTime.class)); // Đảm bảo có gọi hàm gia hạn thời gian
    }

    // =========================================================================
    // NHÓM 2: KIỂM THỬ LUỒNG TÍNH TOÁN TOÁN HỌC AUTOBID (evaluateDeterministicBidding)
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("5. AutoBid kết thúc sớm nếu phòng đấu giá không ở trạng thái RUNNING")
    void testEvaluateAutoBid_AuctionNotRunning_ReturnsImmediately() throws Exception {
        Auction mockAuction = new Auction();
        mockAuction.setStatus(AuctionStatus.FINISHED); // Phòng đấu giá đã kết thúc

        when(auctionDAOMock.getAuctionByAuctionId(1)).thenReturn(mockAuction);

        biddingService.evaluateDeterministicBidding(1);

        // Đảm bảo không có bất kỳ logic tải Bot hay ghi nhận giao dịch nào được thực thi
        verify(autoBidDAOMock, never()).getActiveAutoBidsForAuction(anyInt());
        verify(bidDAOMock, never()).insertBid(any(BidTransaction.class));
    }

    @Test
    @Order(6)
    @DisplayName("6. Một Bot cô đơn đẩy giá thành công lên bằng giá hiện tại + 1 bước giá")
    void testEvaluateAutoBid_SingleBot_Success() throws Exception {
        Auction mockAuction = new Auction();
        mockAuction.setAuctionId(1);
        mockAuction.setStatus(AuctionStatus.RUNNING);
        mockAuction.setHighestBid(new BigDecimal("100000"));
        mockAuction.setBidIncrement(new BigDecimal("20000"));
        mockAuction.setBidderId(99); // Người dẫn đầu hiện tại không phải chủ Bot

        org.example.core.models.users.User mockUser = new org.example.core.models.users.User();
        mockUser.setUserName("BotThong");

        // Giả lập danh sách chỉ có duy nhất 1 bot gác phòng với giá trần kịch sàn là 500k
        List<AutoBidDAO.AutoBidConfig> activeBots = new ArrayList<>();
        AutoBidDAO.AutoBidConfig bot = new AutoBidDAO.AutoBidConfig();
        bot.setUserId(10);
        bot.setMaxBid(new BigDecimal("500000"));
        activeBots.add(bot);

        when(auctionDAOMock.getAuctionByAuctionId(1)).thenReturn(mockAuction);
        when(autoBidDAOMock.getActiveAutoBidsForAuction(1)).thenReturn(activeBots);
        when(userDAOMock.getUserByUserId(10)).thenReturn(mockUser);

        biddingService.evaluateDeterministicBidding(1);

        // Công thức tính toán: 100,000 (Giá hiện tại) + 20,000 (Bước giá) = 120,000
        BigDecimal expectedFinalPrice = new BigDecimal("120000");

        verify(bidDAOMock, times(1)).insertBid(argThat(tx -> tx.getAmount().compareTo(expectedFinalPrice) == 0 && tx.getBidderId() == 10));
        verify(auctionDAOMock, times(1)).updateHighestPriceByItemId(10, expectedFinalPrice);
    }

    @Test
    @Order(7)
    @DisplayName("7. Hai Bot đụng độ: Giá nhảy vọt kịch tính lên bằng giá trần của Bot thua + 1 bước giá")
    void testEvaluateAutoBid_TwoBotsClash_Success() throws Exception {
        Auction mockAuction = new Auction();
        mockAuction.setAuctionId(1);
        mockAuction.setStatus(AuctionStatus.RUNNING);
        mockAuction.setHighestBid(new BigDecimal("100000"));
        mockAuction.setBidIncrement(new BigDecimal("15000"));
        mockAuction.setBidderId(99);

        org.example.core.models.users.User mockUser = new org.example.core.models.users.User();
        mockUser.setUserName("KingBot");

        // Bot 1 đặt trần 600k, Bot 2 đặt trần 400k
        List<AutoBidDAO.AutoBidConfig> activeBots = new ArrayList<>();
        AutoBidDAO.AutoBidConfig bot1 = new AutoBidDAO.AutoBidConfig();
        bot1.setUserId(10);
        bot1.setMaxBid(new BigDecimal("600000"));

        AutoBidDAO.AutoBidConfig bot2 = new AutoBidDAO.AutoBidConfig();
        bot2.setUserId(20);
        bot2.setMaxBid(new BigDecimal("400000"));

        activeBots.add(bot1);
        activeBots.add(bot2);

        when(auctionDAOMock.getAuctionByAuctionId(1)).thenReturn(mockAuction);
        when(autoBidDAOMock.getActiveAutoBidsForAuction(1)).thenReturn(activeBots);
        when(userDAOMock.getUserByUserId(10)).thenReturn(mockUser);

        biddingService.evaluateDeterministicBidding(1);

        // Công thức thép: Trần thằng thua (Bot 2: 400,000) + 1 Bước giá (15,000) = 415,000. Chủ Bot 1 thắng.
        BigDecimal expectedFinalPrice = new BigDecimal("415000");

        verify(bidDAOMock, times(1)).insertBid(argThat(tx -> tx.getAmount().compareTo(expectedFinalPrice) == 0 && tx.getBidderId() == 10));
        verify(auctionDAOMock, times(1)).updateHighestPriceByItemId(10, expectedFinalPrice);
    }
}