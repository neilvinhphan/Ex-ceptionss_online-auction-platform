package org.example.core.models.items;

import org.example.core.models.entities.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public abstract class Item extends Entity {
  private String type;
  private String itemName;
  private BigDecimal startingPrice;
  protected String description;

  // Constructor khi Lấy từ DB lên
  public Item(int id, String type, String itemName, String description, BigDecimal startingPrice) {
    super(id);
    this.type = type;
    this.itemName = itemName;
    this.description = description;
    this.startingPrice = startingPrice;
  }

  public Item(
      int id,
      LocalDateTime createdAt,
      String type,
      String itemName,
      String description,
      BigDecimal startingPrice) {}

  // GETTERS & SETTERS
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public BigDecimal getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(BigDecimal startingPrice) {
    this.startingPrice = startingPrice;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  protected abstract void printInfo();
}
