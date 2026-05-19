package org.example.core.dto.bidDTO;

import java.time.LocalDateTime;

public class BidBroadcastDTO {
  private String type = "NEW_BID"; // Để Client biết đây là tin nhắn cập nhật giá
  private int auctionId;
  private double newPrice;
  private String leaderUsername;
  private LocalDateTime newEndTime;

  // 🔥 THÊM CỜ CHỐT CHẶN: Báo hiệu lượt nhảy giá này có phải do hệ thống AutoBid tự kích hoạt hay không
  private boolean isAutoBidTriggered = false;

  public BidBroadcastDTO() {
    this.type = "NEW_BID";
  }

  // Constructor 3 tham số sẵn có
  public BidBroadcastDTO(int auctionId, double newPrice, String leaderUsername) {
    this.type = "NEW_BID";
    this.auctionId = auctionId;
    this.newPrice = newPrice;
    this.leaderUsername = leaderUsername;
    this.isAutoBidTriggered = false;
  }

  // Constructor đầy đủ tham số kèm thời gian
  public BidBroadcastDTO(int auctionId, double newPrice, String leaderUsername, LocalDateTime newEndTime) {
    this.type = "NEW_BID";
    this.auctionId = auctionId;
    this.newPrice = newPrice;
    this.leaderUsername = leaderUsername;
    this.newEndTime = newEndTime;
    this.isAutoBidTriggered = false;
  }

  // 🔥 THÊM CONSTRUCTOR MỚI: Phục vụ lúc Server xử lý tính toán xong thuật toán AutoBid toán học
  public BidBroadcastDTO(int auctionId, double newPrice, String leaderUsername, LocalDateTime newEndTime, boolean isAutoBidTriggered) {
    this.type = "NEW_BID";
    this.auctionId = auctionId;
    this.newPrice = newPrice;
    this.leaderUsername = leaderUsername;
    this.newEndTime = newEndTime;
    this.isAutoBidTriggered = isAutoBidTriggered;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
  }

  public double getNewPrice() {
    return newPrice;
  }

  public void setNewPrice(double newPrice) {
    this.newPrice = newPrice;
  }

  public String getLeaderUsername() {
    return leaderUsername;
  }

  public void setLeaderUsername(String leaderUsername) {
    this.leaderUsername = leaderUsername;
  }

  public LocalDateTime getNewEndTime() {
    return newEndTime;
  }

  public void setNewEndTime(LocalDateTime newEndTime) {
    this.newEndTime = newEndTime;
  }

  public boolean isAutoBidTriggered() {
    return isAutoBidTriggered;
  }

  public void setAutoBidTriggered(boolean autoBidTriggered) {
    isAutoBidTriggered = autoBidTriggered;
  }
}