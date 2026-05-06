package org.example.core.models.entities;

import org.example.core.shared.enums.WalletTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransaction {
  private int id;
  private int userId;
  private BigDecimal amount;
  private WalletTransactionType type;
  private int referenceId;
  private LocalDateTime createdAt;

  // Constructor cho lúc lấy từ DB lên
  public WalletTransaction(
      int id,
      int userId,
      BigDecimal amount,
      WalletTransactionType type,
      int referenceId,
      LocalDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.amount = amount;
    this.type = type;
    this.referenceId = referenceId;
    this.createdAt = createdAt;
  }

  // Constructor cho lúc chèn mới xuống DB
  public WalletTransaction(
      int userId, BigDecimal amount, WalletTransactionType type, int referenceId) {
    this.userId = userId;
    this.amount = amount;
    this.type = type;
    this.referenceId = referenceId;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
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

  public WalletTransactionType getType() {
    return type;
  }

  public void setType(WalletTransactionType type) {
    this.type = type;
  }

  public int getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(int referenceId) {
    this.referenceId = referenceId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
