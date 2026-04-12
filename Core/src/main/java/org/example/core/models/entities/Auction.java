package org.example.core.models.entities;

import org.example.core.models.items.Item;
import org.example.core.shared.enums.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Auction extends Entity {
  private Item item;
  private AuctionStatus status;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private List<BidTransaction> bidHistory;
  private BidTransaction highestBid;

  // Constructor tạo mới đấu giá
  public Auction(
      Item item,
      AuctionStatus status,
      LocalDateTime startTime,
      LocalDateTime endTime,
      List<BidTransaction> bidHistory,
      BidTransaction highestBid) {
    super(0, LocalDateTime.now());
    this.item = item;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
    this.bidHistory = bidHistory;
    this.highestBid = highestBid;
  }

  // Constructor from DB
  public Auction(
      int id,
      LocalDateTime createdAt,
      Item item,
      AuctionStatus status,
      LocalDateTime startTime,
      LocalDateTime endTime,
      List<BidTransaction> bidHistory,
      BidTransaction highestBid) {
    super(id, createdAt);
    this.item = item;
    this.status = status;
    this.startTime = startTime;
    this.endTime = endTime;
    this.bidHistory = bidHistory;
    this.highestBid = highestBid;
  }

  public void open() {}

  public void start(LocalDateTime now) {}

  public void validateBid(BigDecimal amount) {}

  //  public BidTransaction getHighestBid() {}

  public void close(LocalDateTime now) {}

  public void extendEndTime(int seconds) {}

  //  public boolean isAntiSniping(LocalDateTime at) {}
}
