package org.example.core.dto.admin;

public class AdminBanUserDTO {
  private int adminId;
  private int userId;
  private boolean isBanned;

  public AdminBanUserDTO(int adminId, int userId, boolean isBanned) {
    this.adminId = adminId;
    this.userId = userId;
    this.isBanned = isBanned;
  }

  public int getAdminId() {
    return adminId;
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
}
