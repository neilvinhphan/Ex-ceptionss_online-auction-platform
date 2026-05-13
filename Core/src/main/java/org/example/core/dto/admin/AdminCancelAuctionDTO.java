package org.example.core.dto.admin;

public class AdminCancelAuctionDTO {
    private int auctionId;
    private int adminId;

    public AdminCancelAuctionDTO(int auctionId, int adminId) {
        this.auctionId = auctionId;
        this.adminId = adminId;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }
   }
