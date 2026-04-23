package org.example.Core.models.entities;

import java.math.BigDecimal;
import java.rmi.server.ObjID;
import java.time.LocalDateTime;
import java.util.UUID;

public class BidTransaction {
  private int id;
  private int aucionId; //thuộc về phiên đấu giá nào
  private int userId;
  private BigDecimal bidAmount;
  private LocalDateTime bidTime;
  //
  public BidTransaction(){

  }


    public BidTransaction(int id, int aucionId, int userId, BigDecimal bidAmount, LocalDateTime bidTime) {
        this.id = id;
        this.aucionId = aucionId;
        this.userId = userId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

  public int getAucionId() {
    return aucionId;
  }

  public void setAucionId(int aucionId) {
    this.aucionId = aucionId;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public BigDecimal getBidAmount() {
    return bidAmount;
  }

  public void setBidAmount(BigDecimal bidAmount) {
    this.bidAmount = bidAmount;
  }

  public LocalDateTime getBidTime() {
    return bidTime;
  }

  public void setBidTime(LocalDateTime bidTime) {
    this.bidTime = bidTime;
  }
}
