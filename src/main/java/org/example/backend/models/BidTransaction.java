package org.example.backend.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidTransaction {
  private int id;
  private int auctionId;
  private String bidderUsername;
  private BigDecimal bidAmount;
  private LocalDateTime bidTime;

  public BidTransaction() {}

  public BidTransaction(int id, int auctionId, String bidderUsername, BigDecimal bidAmount, LocalDateTime bidTime) {
    this.id = id;
    this.auctionId = auctionId;
    this.bidderUsername = bidderUsername;
    this.bidAmount = bidAmount;
    this.bidTime = bidTime;
  }

  public int getId() { return id; }
  public void setId(int id) { this.id = id; }

  public int getAuctionId() { return auctionId; }
  public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

  public String getBidderUsername() { return bidderUsername; }
  public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }

  public BigDecimal getBidAmount() { return bidAmount; }
  public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }

  public LocalDateTime getBidTime() { return bidTime; }
  public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }
}
