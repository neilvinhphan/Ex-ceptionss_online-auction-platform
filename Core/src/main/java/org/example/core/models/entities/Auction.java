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
  private long durationMinutes;
  private LocalDateTime endTime;
  private List<BidTransaction> bidHistory;
  private BidTransaction highestBid;

  // Constructor tạo mới đấu giá
  public Auction(Item item, long durationMinutes) {
    super(0, LocalDateTime.now());
    this.item = item;
    this.status = AuctionStatus.WAREHOUSE;
    this.startTime = LocalDateTime.now();
    this.durationMinutes = durationMinutes;
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
      BidTransaction highestBid) {
    super(id, createdAt);
    this.item = item;
    this.status = status;
    this.startTime = startTime;
    this.durationMinutes = durationMinutes;
    this.bidHistory = bidHistory;
    this.highestBid = highestBid;
  }

  public Auction() {

  }

  // Phiên trong kho --> Chỉnh sửa --> Xác nhận --> Start
  public void start(LocalDateTime now) {
    if (this.status == AuctionStatus.WAREHOUSE) {
      this.status = AuctionStatus.RUNNING;
      this.startTime = LocalDateTime.now();
      this.endTime = this.startTime.plusMinutes(this.durationMinutes);
    } else if (this.status == AuctionStatus.RUNNING) {
      throw new RuntimeException("Phiên đấu giá đang diễn ra!");
    } else {
      throw new RuntimeException("Phiên đấu giá đã kết thúc!");
    }
  }

  // Hết time --> Đóng phiên
  public void close(LocalDateTime now) {
    if (this.status == AuctionStatus.RUNNING
        && (now.isEqual(this.endTime) || now.isAfter(this.endTime))) {
      this.status = AuctionStatus.FINISHED;
    }
  }

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

  //  public BidTransaction getHighestBid() {}

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
    this.status = status;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public long getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(long durationMinutes) {
    this.durationMinutes = durationMinutes;
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

  public BidTransaction getHighestBid() {
    return highestBid;
  }

  public void setHighestBid(BidTransaction highestBid) {
    this.highestBid = highestBid;
  }
}
