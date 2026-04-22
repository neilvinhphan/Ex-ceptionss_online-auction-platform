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

  // Phiên trong kho --> Pending xác nhận
  public void open(LocalDateTime now) {
    if (this.status == AuctionStatus.WAREHOUSE && !now.isBefore(this.startTime)) {
      this.status = AuctionStatus.PENDING;
    }
  }

  // Phiên Pending --> Running
  public void start(LocalDateTime now) {
    if (this.status == AuctionStatus.PENDING && now.isBefore(this.startTime)) {
      this.status = AuctionStatus.RUNNING;
    }
  }

  // Đóng phiên
  public void close(LocalDateTime now) {
    if (this.status == AuctionStatus.RUNNING && now.isAfter(this.endTime)) {
      this.status = AuctionStatus.FINISHED;
    }
  }

  public void validateBid(LocalDateTime now, BigDecimal amount) throws Exception {
    // Check trạng thái
    if (this.status != AuctionStatus.RUNNING) {
      throw new Exception("The auction has ended.");
    }

    // Check thời gian
    if (now.isBefore(this.startTime) || !now.isBefore() )
  }

  //  public BidTransaction getHighestBid() {}

  public void extendEndTime(int seconds) {}

  //  public boolean isAntiSniping(LocalDateTime at) {}
}
