package org.example.core.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BidTransaction {
  private BigDecimal amount;
  private LocalDateTime time;
  private int bidderID;
  private String bidderName;
  private int bidId;
  private int auctionId;

  public BidTransaction() {}

  public BidTransaction(int bidId, int bidderID, int auctionId, BigDecimal amount, LocalDateTime bidTime, String bidderName) {
    this.bidId = bidId;
    this.bidderID = bidderID;
    this.auctionId = auctionId;
    this.amount = amount;
    this.time =  bidTime;
    this.bidderName = bidderName;
  }

  public BidTransaction(BigDecimal amount, LocalDateTime time, int bidderID) {
    this.amount = amount;
    this.time = time;
    this.bidderID = bidderID;
  }

  public void setBidAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public void setBidTime(LocalDateTime time) {
    this.time = time;
  }

  public void setBidderId(int bidderID) {
    this.bidderID = bidderID;
  }

  public void setBidId(int bidId) {
    this.bidId = bidId;
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

  public int getBidId() {
    return bidId;
  }

  public int getAuctionId() {
    return auctionId;
  }
  //  public int compareTo(BidTransaction other) {}
  public String getBidderName() {
    return bidderName;
  }

  public void setBidderName(String bidderName) {
    this.bidderName = bidderName;
  }
}
