package org.example.core.dto.admin;

import java.math.BigDecimal;

public class AiEvaluationDTO {
  private boolean isSafe;
  private BigDecimal suggestedPrice;
  private String reason;

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

  public String getReason() {
    return reason;
  }
}
