package org.example.core.dto.auctionDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionDetailDTO {
  private int auctionId;
  private String itemName;
  private String description;
  private BigDecimal currentPrice;
  private BigDecimal bidStep; // Bước giá
  private String status; // Trạng thái sàn (Đang diễn ra, Chờ, Kết thúc...)
  private String highestBidder;
  private LocalDateTime endTime;

  public AuctionDetailDTO(
      int auctionId,
      String itemName,
      String description,
      BigDecimal currentPrice,
      BigDecimal bidStep,
      String status,
      String highestBidder,
      LocalDateTime endTime) {
    this.auctionId = auctionId;
    this.itemName = itemName;
    this.description = description;
    this.currentPrice = currentPrice;
    this.bidStep = bidStep;
    this.status = status;
    this.highestBidder = highestBidder;
    this.endTime = endTime;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
