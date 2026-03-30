package org.example.backend.models;

import org.example.backend.models.bidding.BidTransaction;
import org.example.backend.models.enums.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Auction {
  private Item item;
  private AuctionStatus status;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private List<BidTransaction> bidHistory;
  private BidTransaction highestBid;

  public void open() {}

  public void start(LocalDateTime now) {}

  //  public boolean placeBid(User bidder, BigDecimal amount) {}

  public void validateBid(BigDecimal amount) {}

  //  public BidTransaction getHighestBid() {}

  public void close(LocalDateTime now) {}

  public void extendEndTime(int seconds) {}

  //  public boolean isAntiSniping(LocalDateTime at) {}
}
