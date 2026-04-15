package org.example.Core.models.items;

import org.example.Core.models.entities.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public abstract class Item extends Entity {
  private String type;
  private String itemName;
  private BigDecimal startingPrice;
  protected String description;
  private  int sellerID;

  // Constructor khi Lấy từ DB lên
  public Item(int id,int sellerID, String type, String itemName, String description, BigDecimal startingPrice) {
    super(id);
    this.sellerID = sellerID;
    this.type = type;
    this.itemName = itemName;
    this.description = description;
    this.startingPrice = startingPrice;
  }

  // Constructor khi tạo Item
  public Item(
      int id,
      int sellerID,
      LocalDateTime createdAt,
      String type,
      String itemName,
      String description,
      BigDecimal startingPrice) {
    super(id, createdAt);
    this.sellerID = sellerID;
    this.type = type;
    this.itemName = itemName;
    this.description = description;
    this.startingPrice = startingPrice;
  }

  public Item() {}

  public Item() {}

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

  public void setSellerID(int sellerID) {
    this.sellerID = sellerID;
  }

  public int getSellerID() {
    return sellerID;
  }

  protected abstract void printInfo();
}
