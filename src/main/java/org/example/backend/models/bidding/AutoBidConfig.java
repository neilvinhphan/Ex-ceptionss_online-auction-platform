package org.example.backend.models.bidding;

import org.example.backend.models.Auction;
import org.example.backend.models.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AutoBidConfig {
  private Auction aucntionId;
  private User bidderID;
  private BigDecimal maxBid;
  private BigDecimal increment;
  private LocalDateTime createdAt;
  private boolean active;
}
