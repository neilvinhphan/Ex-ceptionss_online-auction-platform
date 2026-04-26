package org.example.core.dto;

import org.example.core.models.items.Item;

public class AuctionRequestDTO {
  private Item item;
  private long durationMinutes;

  public AuctionRequestDTO(Item item, long durationMinutes) {
    this.item = item;
    this.durationMinutes = durationMinutes;
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
}
