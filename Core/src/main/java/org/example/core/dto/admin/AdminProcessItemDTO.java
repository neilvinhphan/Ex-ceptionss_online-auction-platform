package org.example.core.dto.admin;

public class AdminProcessItemDTO {
    private int adminId;
    private boolean isApproved;
    private int itemId;

    public AdminProcessItemDTO(int adminId, boolean isApproved, int itemId) {
        this.adminId = adminId;
        this.isApproved = isApproved;
        this.itemId = itemId;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }
}