package org.example.core.dto.admin;

public class AdminApproveAuctionDTO {
    private int adminId;
    private int auctionId;

    public AdminApproveAuctionDTO(int adminId, int auctionId) {
        this.adminId = adminId;
        this.auctionId = auctionId;
    }

    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }
    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }
}
