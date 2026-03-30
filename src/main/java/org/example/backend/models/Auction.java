package org.example.backend.models;

import org.example.backend.models.enums.AuctionStatus;

import java.time.LocalDateTime;
import java.util.List;

public class Auction {
  private Item item;
  private AuctionStatus status;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private List<BidTransaction> bidHistory;
  private BidTransaction highestBid;
}
