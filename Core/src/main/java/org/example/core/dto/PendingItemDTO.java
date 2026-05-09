package org.example.core.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class PendingItemDTO implements Serializable {
    private int auctionId;
    private String itemName;
    private BigDecimal winPrice;
    private String endDate;

    public PendingItemDTO() {}

    public PendingItemDTO(int auctionId, String itemName, BigDecimal winPrice, String endDate) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.winPrice = winPrice;
        this.endDate = endDate;
    }
    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public BigDecimal getWinPrice() { return winPrice; }
    public void setWinPrice(BigDecimal winPrice) { this.winPrice = winPrice; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}