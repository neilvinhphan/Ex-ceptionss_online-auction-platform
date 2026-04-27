package org.example.server.services;

import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.shared.enums.AuctionStatus;
import org.example.server.daos.AuctionDAO;
import org.example.server.daos.BidDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BiddingServiceTest {

    private BiddingService biddingService;
    private BidDAO bidDAO;
    private AuctionDAO auctionDAO;

    @BeforeEach
    void setUp() {
        bidDAO = mock(BidDAO.class);
        auctionDAO = mock(AuctionDAO.class);
        biddingService = new BiddingService(bidDAO, auctionDAO);
    }

    @Test
    @DisplayName("Place bid - success when bid is valid")
    void placeBid_success() throws Exception {
        int auctionId = 1;
        int userId = 10;
        BigDecimal amount = new BigDecimal("120");

        Auction auction = runningAuction();
        when(auctionDAO.getAuctionById(auctionId)).thenReturn(auction);
        when(bidDAO.getCurrentPrice(auctionId)).thenReturn(new BigDecimal("100"));
        when(auctionDAO.getBidIncrementByAuctionId(auctionId)).thenReturn(new BigDecimal("10"));
        when(bidDAO.updateNewBid(auctionId, userId, amount)).thenReturn(true);
        when(bidDAO.updateCurrentPrice(auctionId, amount)).thenReturn(true);

        assertTrue(biddingService.placeBid(auctionId, userId, amount));
    }

    @Test
    @DisplayName("Place bid - fail when auction does not exist")
    void placeBid_auctionNotFound() {
        int auctionId = 1;
        int userId = 10;
        BigDecimal amount = new BigDecimal("100");

        when(auctionDAO.getAuctionById(auctionId)).thenReturn(null);

        Exception ex = assertThrows(Exception.class, () -> biddingService.placeBid(auctionId, userId, amount));
        assertEquals("Không tìm thấy phiên đấu giá.", ex.getMessage());
    }

    @Test
    @DisplayName("Place bid - fail when bid lower than current highest")
    void placeBid_lowerThanCurrentHighest() {
        int auctionId = 1;
        int userId = 10;
        BigDecimal amount = new BigDecimal("105");

        Auction auction = runningAuction();
        when(auctionDAO.getAuctionById(auctionId)).thenReturn(auction);
        when(bidDAO.getCurrentPrice(auctionId)).thenReturn(new BigDecimal("100"));
        when(auctionDAO.getBidIncrementByAuctionId(auctionId)).thenReturn(new BigDecimal("10"));

        Exception ex = assertThrows(Exception.class, () -> biddingService.placeBid(auctionId, userId, amount));
        assertTrue(ex.getMessage().startsWith("Giá đặt phải >="));
    }

    @Test
    @DisplayName("Place bid - fail when update current price fails")
    void placeBid_updateCurrentPriceFails() {
        int auctionId = 1;
        int userId = 10;
        BigDecimal amount = new BigDecimal("120");

        Auction auction = runningAuction();
        when(auctionDAO.getAuctionById(auctionId)).thenReturn(auction);
        when(bidDAO.getCurrentPrice(auctionId)).thenReturn(new BigDecimal("100"));
        when(auctionDAO.getBidIncrementByAuctionId(auctionId)).thenReturn(new BigDecimal("10"));
        when(bidDAO.updateNewBid(auctionId, userId, amount)).thenReturn(true);
        when(bidDAO.updateCurrentPrice(auctionId, amount)).thenReturn(false);

        Exception ex = assertThrows(Exception.class, () -> biddingService.placeBid(auctionId, userId, amount));
        assertEquals("Không thể cập nhật giá hiện tại.", ex.getMessage());
    }

    @Test
    @DisplayName("Get bid history - returns all bids in expected order")
    void getBidHistory_success() {
        int auctionId = 1;
        List<BidTransaction> history =
                List.of(
                        new BidTransaction(new BigDecimal("100"), LocalDateTime.now().minusMinutes(2), 1),
                        new BidTransaction(new BigDecimal("200"), LocalDateTime.now().minusMinutes(1), 2));
        when(bidDAO.getBidTransactionByAuctionId(auctionId)).thenReturn(history);

        List<BidTransaction> actual = biddingService.getBidHistory(auctionId);

        assertEquals(2, actual.size());
        assertEquals(new BigDecimal("100"), actual.get(0).getAmount());
        assertEquals(new BigDecimal("200"), actual.get(1).getAmount());
    }

    private Auction runningAuction() {
        Auction auction = new Auction();
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setStartTime(LocalDateTime.now().minusMinutes(1));
        auction.setEndTime(LocalDateTime.now().plusMinutes(10));
        return auction;
    }
}
