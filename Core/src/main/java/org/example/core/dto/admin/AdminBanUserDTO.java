package org.example.core.dto.admin;

public class AdminBanUserDTO {
    private int adminId; // ID của người đang thực hiện lệnh này
    private int userId;  // ID của kẻ bị khóa mõm
    private boolean isBanned;

    public AdminBanUserDTO(int adminId, int userId, boolean isBanned) {
        this.adminId = adminId;
        this.userId = userId;
        this.isBanned = isBanned;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        isBanned = banned;
    }
}
