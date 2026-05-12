package org.example.core.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PendingPaymentsDTO implements Serializable {
    private int userId;
    private int auctionId;
    private String itemName;
    private BigDecimal winPrice;
    private LocalDateTime endDate;

    public PendingPaymentsDTO() {}

    public PendingPaymentsDTO(int userId) {
        this.userId = userId;
    }

    public PendingPaymentsDTO(int auctionId, String itemName, BigDecimal winPrice, LocalDateTime endDate) {
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

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate;}

    public int getUserId() {return userId;}
    public void setUserId(int userId) {this.userId = userId;}
}