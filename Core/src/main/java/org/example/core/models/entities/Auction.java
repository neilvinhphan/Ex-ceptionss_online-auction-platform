package org.example.core.models.entities;

import org.example.core.models.items.Item;
import org.example.core.shared.enums.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Auction extends Entity {
  private int itemId;
  private int auctionId;
  private int bidderId;
  private Item item;
  private AuctionStatus status;
  private LocalDateTime startTime;
  private long durationMinutes;
  private LocalDateTime endTime;
  private List<BidTransaction> bidHistory;
  private BigDecimal highestBid;
  private BigDecimal bidIncrement;
  private int ownerId;
  private String itemName;
  private String type;
  // Constructor tạo mới đấu giá
  public Auction(
      Item item, LocalDateTime startTime, long durationMinutes, BigDecimal bidIncrement) {
    super(LocalDateTime.now());
    this.item = item;
    this.status = AuctionStatus.RUNNING;
    this.startTime = startTime;
    this.durationMinutes = durationMinutes;
    this.bidIncrement = bidIncrement;
  }

  // Constructor from DB
  public Auction(
      int id,
      LocalDateTime createdAt,
      Item item,
      AuctionStatus status,
      LocalDateTime startTime,
      long durationMinutes,
      List<BidTransaction> bidHistory,
      BigDecimal highestBid,
      int bidderId) {
    super(createdAt);
    this.item = item;
    this.status = status;
    this.startTime = startTime;
    this.durationMinutes = durationMinutes;
    this.bidHistory = bidHistory;
    this.highestBid = highestBid;
    this.bidderId = bidderId;
  }

  public Auction() {}

  public void validateBid(LocalDateTime now, BigDecimal amount) throws Exception {
    // Check trạng thái
    if (this.status != AuctionStatus.RUNNING) {
      throw new Exception("Phiên đấu giá đã kết thúc!");
    }

    // Check thời gian
    if (now.isBefore(this.startTime) || now.isAfter(this.endTime)) {
      throw new Exception("Đã hết thời gian đặt giá!");
    }
  }

  // Anti-Sniping
  public void extendEndTime(long seconds) {
    if (this.endTime != null) {
      this.endTime = this.endTime.plusSeconds(seconds);
    }
  }

  public boolean isAntiSniping(LocalDateTime now, long thresholdSeconds) {
    if (this.endTime == null || now.isAfter(this.endTime)) {
      return false;
    }

    LocalDateTime snipingWindowStart = this.endTime.minusSeconds(thresholdSeconds);
    boolean rangeCheck =
        (now.isEqual(snipingWindowStart) || now.isAfter(snipingWindowStart))
            && (now.isBefore(this.endTime) || now.isEqual(this.endTime));

    return rangeCheck;
  }

  public long getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(long durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public int getBidderId() {
    return bidderId;
  }

  public void setBidderId(int bidderId) {
    this.bidderId = bidderId;
  }

  public int getItemId() {
    return itemId;
  }

  public void setItemId(int itemId) {
    this.itemId = itemId;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public AuctionStatus getStatus() {
    return status;
  }

  public void setStatus(AuctionStatus status) {
    if (status == null) {
      System.out.println("WARNING: Đang cố tình set Status thành NULL cho Auction ID: " + this.auctionId);
    }
    this.status = status;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public List<BidTransaction> getBidHistory() {
    return bidHistory;
  }

  public void setBidHistory(List<BidTransaction> bidHistory) {
    this.bidHistory = bidHistory;
  }

  public BigDecimal getHighestBid() {
    return highestBid;
  }

  public void setHighestBid(BigDecimal highestBid) {
    this.highestBid = highestBid;
  }

  public BigDecimal getBidIncrement() {
    return bidIncrement;
  }

  public void setBidIncrement(BigDecimal bidIncrement) {
    this.bidIncrement = bidIncrement;
  }

  public void setItemName(String itemName) {this.itemName = itemName;}

  public String getItemName() {return itemName;}
  public void setType(String type) {this.type = type;}
  public String getType() {return type;}
  public void setOwnerId(int ownerId) {this.ownerId = ownerId;}

  public int getOwnerId() {return ownerId;}
}
