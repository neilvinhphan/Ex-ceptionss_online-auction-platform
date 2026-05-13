package org.example.core.dto.auctionDTO;

import org.example.core.models.items.Item;

import java.math.BigDecimal;

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

  public long getDurationMinutes() {
    return durationMinutes;
  }

  public BigDecimal getBidIncrement() {
    return bidIncrement;
  }
}
