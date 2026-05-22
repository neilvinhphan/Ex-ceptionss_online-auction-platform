package org.example.core.dto.bidDTO;

import java.math.BigDecimal;

public class AutoBidRequestDTO {
    private int auctionId;
    private int userId;
    private BigDecimal maxBid; // Số tiền trần tối đa User sẵn sàng trả

    public AutoBidRequestDTO() {
    }

    public AutoBidRequestDTO(int auctionId, int userId, BigDecimal maxBid) {
        this.auctionId = auctionId;
        this.userId = userId;
        this.maxBid = maxBid;
    }

    // --- HÀM TRÍCH XUẤT DỮ LIỆU (GETTER / SETTER) ---
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

    public void setMaxBid(BigDecimal maxBid) {
        this.maxBid = maxBid;
    }
}