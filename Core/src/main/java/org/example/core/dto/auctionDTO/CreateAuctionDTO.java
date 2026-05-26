package org.example.core.dto.auctionDTO;

import org.example.core.models.items.Item;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateAuctionDTO {
  private Item item;
  private long durationMinutes;
  private BigDecimal bidIncrement;
  private LocalDateTime startTime;

  public CreateAuctionDTO(
      Item item, long durationMinutes, BigDecimal bidIncrement, LocalDateTime startTime) {
    this.item = item;
    this.durationMinutes = durationMinutes;
    this.bidIncrement = bidIncrement;
    this.startTime = startTime;
  }

  public CreateAuctionDTO() {
  }

  public LocalDateTime getStartTime() {
    return startTime;
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

  public void setItem(Item item) {
    this.item = item;
  }

  public void setDurationMinutes(long durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public void setBidIncrement(BigDecimal bidIncrement) {
    this.bidIncrement = bidIncrement;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }
}
