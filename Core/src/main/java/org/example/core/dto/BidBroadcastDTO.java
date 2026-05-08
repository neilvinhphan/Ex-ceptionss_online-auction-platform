package org.example.core.dto;

import java.time.LocalDateTime;

public class BidBroadcastDTO {
    private String type = "NEW_BID"; // Để Client biết đây là tin nhắn cập nhật giá
    private int auctionId;
    private double newPrice;
    private String leaderUsername;
    private LocalDateTime newEndTime;

    public BidBroadcastDTO() {
        this.type = "NEW_BID";
    }

    // 2. Constructor 3 tham số mà ClientHandler đang gọi
    public BidBroadcastDTO(int auctionId, double newPrice, String leaderUsername) {
        this.type = "NEW_BID"; // Cứ tin nhắn giá là gắn nhãn này
        this.auctionId = auctionId;
        this.newPrice = newPrice;
        this.leaderUsername = leaderUsername;
    }

    public BidBroadcastDTO(int auctionId, double newPrice, String leaderUsername, LocalDateTime newEndTime) {
        this.type = "NEW_BID";
        this.auctionId = auctionId;
        this.newPrice = newPrice;
        this.leaderUsername = leaderUsername;
        this.newEndTime = newEndTime; // Đã gán dữ liệu thời gian đàng hoàng
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
}
