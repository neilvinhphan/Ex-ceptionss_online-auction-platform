package org.example.core.dto.userDTO;

import java.math.BigDecimal;

public class DepositRequestDTO {
    private int userId;
    private BigDecimal amount;
    private String password;
    public DepositRequestDTO(int userId, BigDecimal amount,String password) {
        this.userId = userId;
        this.amount = amount;
        this.password = password;
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
    public String getPassword() { return password; }
    public void setPassword(String password){this.password=password;}
}
