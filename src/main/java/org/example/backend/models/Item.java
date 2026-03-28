package org.example.backend.models;

import java.time.LocalDateTime;

public abstract class Item extends Entity {
  private String type;
  private String itemName;
  private double startingPrice;
  protected String description;

  // Constructor khi Lấy từ DB lên
  public Item(
      int id,
      LocalDateTime createdAt,
      String type,
      String itemName,
      String description,
      double startingPrice) {
    super(id, createdAt);
    this.type = type;
    this.itemName = itemName;
    this.description = description;
    this.startingPrice = startingPrice;
  }

  // GETTERS & SETTERS
  public String getType() {
    return type;
  }

  public void setType(String newType) {
    this.type = type;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(double startingPrice) {
    this.startingPrice = startingPrice;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  abstract void printInfo();
}
