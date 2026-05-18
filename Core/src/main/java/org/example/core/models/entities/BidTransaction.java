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

  public BidTransaction() {}

  public BidTransaction(
      int bidId,
      int bidderID,
      int auctionId,
      BigDecimal amount,
      LocalDateTime bidTime,
      String bidderName) {
    this.bidId = bidId;
    this.bidderID = bidderID;
    this.auctionId = auctionId;
    this.amount = amount;
    this.time = bidTime;
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

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public void setTime(LocalDateTime time) {
    this.time = time;
  }

  public int getBidderID() {
    return bidderID;
  }

  public void setBidderID(int bidderID) {
    this.bidderID = bidderID;
  }

  public void setBidderName(String bidderName) {
    this.bidderName = bidderName;
  }

  public int getBidId() {
    return bidId;
  }

  public void setBidId(int bidId) {
    this.bidId = bidId;
  }
}
