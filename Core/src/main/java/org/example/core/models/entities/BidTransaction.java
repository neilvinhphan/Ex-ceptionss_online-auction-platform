package org.example.core.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidTransaction extends Entity {
  private BigDecimal amount;
  private LocalDateTime time;
  private int bidderID;
  private String bidderName;
  private int bidId;
  private int auctionId;

  public BidTransaction(BigDecimal amount, LocalDateTime time, int bidderID, String bidderName) {
    this.amount = amount;
    this.time = time;
    this.bidderID = bidderID;
    this.bidderName = bidderName;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDateTime getTime() {
    return time;
  }

  public int getBidderId() {
    return bidderID;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public String getBidderName() {
    return bidderName;
  }
}
