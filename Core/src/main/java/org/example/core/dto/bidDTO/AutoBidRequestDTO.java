package org.example.core.dto.bidDTO;

import java.math.BigDecimal;

public class AutoBidRequestDTO {
    private int auctionId;
    private int userId;
    private BigDecimal maxBid;

    public AutoBidRequestDTO(int auctionId, int userId, BigDecimal maxBid) {
        this.auctionId = auctionId;
        this.userId = userId;
        this.maxBid = maxBid;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public BigDecimal getMaxBid() {
        return maxBid;
    }
}