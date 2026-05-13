package org.example.core.dto.auctionDTO;

import java.io.Serializable;

public class PendingAuctionDTO implements Serializable {
  private Integer adminId;

  public PendingAuctionDTO() {}

  public PendingAuctionDTO(Integer adminId) {
    this.adminId = adminId;
  }

  public Integer getAdminId() {
    return adminId;
  }

  public void setAdminId(Integer adminId) {
    this.adminId = adminId;
  }
}
