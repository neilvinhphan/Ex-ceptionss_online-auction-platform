package org.example.backend.models;

import java.time.LocalDateTime;

public class Bidder extends User {
  protected double walletBalance;

  public Bidder(String userName, String email, int id, double walletBalance) {
    super(userName, email, id);
    this.walletBalance = walletBalance;
  }

  public double getWalletBalance() {
    return walletBalance;
  }
}
