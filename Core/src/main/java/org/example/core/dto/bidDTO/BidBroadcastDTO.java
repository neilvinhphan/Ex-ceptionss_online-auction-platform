package org.example.core.dto.bidDTO;

import java.time.LocalDateTime;

public class BidBroadcastDTO {
  private String type = "NEW_BID";
  private int auctionId;
  private double newPrice;
  private String leaderUsername;
  private LocalDateTime newEndTime;
  private boolean isAutoBidTriggered = false;

  public BidBroadcastDTO(int auctionId, double newPrice, String leaderUsername, LocalDateTime newEndTime) {
    this.type = "NEW_BID";
    this.auctionId = auctionId;
    this.newPrice = newPrice;
    this.leaderUsername = leaderUsername;
    this.newEndTime = newEndTime;
    this.isAutoBidTriggered = false;
  }

  public BidBroadcastDTO(int auctionId, double newPrice, String leaderUsername, LocalDateTime newEndTime, boolean isAutoBidTriggered) {
    this.type = "NEW_BID";
    this.auctionId = auctionId;
    this.newPrice = newPrice;
    this.leaderUsername = leaderUsername;
    this.newEndTime = newEndTime;
    this.isAutoBidTriggered = isAutoBidTriggered;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
  }

  public double getNewPrice() {
    return newPrice;
  }

  public String getLeaderUsername() {
    return leaderUsername;
  }

  public LocalDateTime getNewEndTime() {
    return newEndTime;
  }

  public boolean isAutoBidTriggered() {
    return isAutoBidTriggered;
  }
}