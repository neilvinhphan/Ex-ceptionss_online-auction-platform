package org.example.backend.models;

import java.time.LocalDateTime;

public class Bidder {
  protected double walletBalance;

  public Bidder(String userName, String email, int id, double walletBalance) {
    this.walletBalance = walletBalance;
  }

  public double getWalletBalance() {
    return walletBalance;
  }
}
