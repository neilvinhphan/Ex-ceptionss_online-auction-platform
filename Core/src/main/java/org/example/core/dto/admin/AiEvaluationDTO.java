package org.example.core.dto.admin;

import java.math.BigDecimal;

public class AiEvaluationDTO {
  private boolean isSafe; // AI xác nhận đồ này có sạch không
  private BigDecimal suggestedPrice; // Mức giá AI đề xuất
  private String reason; // Giải thích của AI (tại sao cấm hoặc tại sao định giá thế)

  public AiEvaluationDTO() {}

  public AiEvaluationDTO(boolean isSafe, BigDecimal suggestedPrice, String reason) {
    this.isSafe = isSafe;
    this.suggestedPrice = suggestedPrice;
    this.reason = reason;
  }

  // Getters and Setters
  public boolean isSafe() {
    return isSafe;
  }

  public void setSafe(boolean safe) {
    isSafe = safe;
  }

  public BigDecimal getSuggestedPrice() {
    return suggestedPrice;
  }

  public void setSuggestedPrice(BigDecimal suggestedPrice) {
    this.suggestedPrice = suggestedPrice;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
