package org.example.core.dto;

import java.math.BigDecimal;

public class DepositRequestDTO {
    private int userId;
    private BigDecimal amount;
    public DepositRequestDTO(int userId, BigDecimal amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
    this.userId = userId;
  }
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
