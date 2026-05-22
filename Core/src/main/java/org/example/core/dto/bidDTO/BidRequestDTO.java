package org.example.core.dto.bidDTO;

import java.math.BigDecimal;

public class BidRequestDTO {
  private int auctionId;
  private int userId; // Hoặc username của người đang đăng nhập
  private BigDecimal bidAmount; // Số tiền đặt

  public BidRequestDTO(int auctionId, int userId, BigDecimal bidAmount) {
    this.auctionId = auctionId;
    this.userId = userId;
    this.bidAmount = bidAmount;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
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
}
