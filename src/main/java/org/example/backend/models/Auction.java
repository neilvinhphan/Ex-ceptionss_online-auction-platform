package org.example.backend.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Auction {
  private int id;
  private String itemName;
  private String itemDescription;
  private String status;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private BigDecimal startingPrice;
  private BigDecimal currentPrice;
  private String leaderUsername;
  private List<BidTransaction> bidHistory;

  public Auction() {}

  public Auction(int id, String itemName, String itemDescription, String status,
                 LocalDateTime startTime, LocalDateTime endTime,
                 BigDecimal startingPrice, BigDecimal currentPrice, String leaderUsername) {
    this.id = id;
    this.itemName = itemName;
    this.itemDescription = itemDescription;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
    this.startingPrice = startingPrice;
    this.currentPrice = currentPrice;
    this.leaderUsername = leaderUsername;
  }

  public int getId() { return id; }
  public void setId(int id) { this.id = id; }

  public String getItemName() { return itemName; }
  public void setItemName(String itemName) { this.itemName = itemName; }

  public String getItemDescription() { return itemDescription; }
  public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public LocalDateTime getStartTime() { return startTime; }
  public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

  public LocalDateTime getEndTime() { return endTime; }
  public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

  public BigDecimal getStartingPrice() { return startingPrice; }
  public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }

  public BigDecimal getCurrentPrice() { return currentPrice; }
  public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

  public String getLeaderUsername() { return leaderUsername; }
  public void setLeaderUsername(String leaderUsername) { this.leaderUsername = leaderUsername; }

  public List<BidTransaction> getBidHistory() { return bidHistory; }
  public void setBidHistory(List<BidTransaction> bidHistory) { this.bidHistory = bidHistory; }
}
