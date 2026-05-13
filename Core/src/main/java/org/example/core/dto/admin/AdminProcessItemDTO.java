package org.example.core.dto.admin;

public class AdminProcessItemDTO {
    private int itemId;
    private boolean isApproved; // true là Duyệt, false là Từ chối
    private int adminId;


    public AdminProcessItemDTO(int itemId, boolean isApproved, int adminId) {
        this.itemId = itemId;
        this.isApproved = isApproved;
        this.adminId = adminId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
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
}
