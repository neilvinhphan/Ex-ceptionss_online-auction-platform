package org.example.server.core.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BidTransaction {
  private BigDecimal amount;
  private LocalDateTime time;
  private UUID bidderID;

  public BidTransaction(BigDecimal amount, LocalDateTime time, UUID bidderID) {
    this.amount = amount;
    this.time = time;
    this.bidderID = bidderID;
  }

  //  public int compareTo(BidTransaction other) {}
}
