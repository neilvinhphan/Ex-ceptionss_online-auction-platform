package org.example.backend.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AutoBidConfig {
  private Auction aucntionId;
  private User bidderID;
  private BigDecimal maxBid;
  private BigDecimal increment;
  private LocalDateTime createdAt;
  private boolean active;
}
