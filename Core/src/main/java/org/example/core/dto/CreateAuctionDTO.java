package org.example.core.dto;

import org.example.core.models.items.Item;

import java.math.BigDecimal;
import java.util.List;

public class CreateAuctionDTO {
    private Item item;
    private long durationMinutes;
    private BigDecimal bidIncrement;

    public CreateAuctionDTO(Item item, long durationMinutes, BigDecimal bidIncrement) {
        this.item = item;
        this.durationMinutes = durationMinutes;
        this.bidIncrement = bidIncrement;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getBidIncrement() {
        return bidIncrement;
    }

    public void setBidIncrement(BigDecimal bidIncrement) {
        this.bidIncrement = bidIncrement;
    }
}
