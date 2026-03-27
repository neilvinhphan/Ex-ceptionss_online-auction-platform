package org.example.backend.models;

public class Seller extends User {
  // Thuộc tính riêng của seller
  private double rating;

  // Khởi tạo
  public Seller(int id, String userName, String phone, String email, double rating) {
    super(id, userName, phone, email);
    this.rating = rating;
  }

  // Getters
  public double getRating() {
    return rating;
  }

  // Setters
  public void setRating(double rating) {
    this.rating = rating;
  }

  // Methods
  public void createItem(String type, String name, String desc, double startingPrice) {}
}
