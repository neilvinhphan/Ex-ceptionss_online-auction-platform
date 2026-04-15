package org.example.core.models.users;

public class SellerProfile {
  protected double rating;

  public SellerProfile() {}

  public SellerProfile(double rating) {
    this.rating = rating;
  }

  public double getRating() {
    return rating;
  }

  public void setRating(double rating) {
    this.rating = rating;
  }
}