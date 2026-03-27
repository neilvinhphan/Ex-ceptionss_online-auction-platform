package org.example.backend.models;

import java.time.LocalDateTime;

public class Bidder extends User {
  protected double walletBalance;

  public Bidder(int id, String userName, String phone, String email, double walletBalance) {
    super(id, userName, phone, email);
    this.walletBalance = walletBalance;
  }

  public double getWalletBalance() {
    return walletBalance;
  }
}
