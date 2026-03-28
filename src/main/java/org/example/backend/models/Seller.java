package org.example.backend.models;

import java.time.LocalDateTime;

public class Seller extends User {
  // Thuộc tính riêng của seller
  private double rating;
  protected String artist;
  protected int creationYear;
  protected String brand;
  protected int warrantyMonths;
  protected String condition;

  // Khởi tạo
  public Seller(String userName, String phone, String email, double rating) {
    super(userName, phone, email);
    this.rating = rating;
  }

  // GETTERS & SETTERS
  public double getRating() {
    return rating;
  }

  public void setRating(double rating) {
    this.rating = rating;
  }

  // METHODS
  public Item createItem(String type, String name, String desc, double startingPrice) {
    Item newItem = null;

    if (type.equalsIgnoreCase("Vehicle")) {
      newItem = new VehicleItem(0, null, type, name, desc, startingPrice);
    } else if (type.equalsIgnoreCase("Art")) {
      newItem = new ArtItem(0, null, type, name, artist, creationYear, desc, startingPrice);
    } else if (type.equalsIgnoreCase("Electronics")) {
      newItem = new ElectronicsItem(0, null, type, name, brand, warrantyMonths, condition, desc, startingPrice);
    }

    return newItem;
  }

  public void updateItem(
      Item needChange, String newType, String newName, String newDesc, double newStartingPrice) {
    needChange.setType(newType);
    needChange.setItemName(newName);
    needChange.setDescription(newDesc);
  }

  public void removeItem() {}
}
