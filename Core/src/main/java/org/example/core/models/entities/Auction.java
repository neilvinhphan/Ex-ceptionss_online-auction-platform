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

  public void open() {}

  public void start(LocalDateTime now) {}

  public void validateBid(BigDecimal amount) {}

  //  public BidTransaction getHighestBid() {}

  public void close(LocalDateTime now) {}

  public void extendEndTime(int seconds) {}

  //  public boolean isAntiSniping(LocalDateTime at) {}
}
