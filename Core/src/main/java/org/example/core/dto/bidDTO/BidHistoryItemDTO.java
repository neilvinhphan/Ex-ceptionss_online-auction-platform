package org.example.core.dto.bidDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidHistoryItemDTO {
    private String username;
    private BigDecimal bidAmount;
    private LocalDateTime bidTime;

    public BidHistoryItemDTO(String username, BigDecimal bidAmount, LocalDateTime bidTime) {
        this.username = username;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
}
