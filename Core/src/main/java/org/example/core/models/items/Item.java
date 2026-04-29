package org.example.core.models.items;

import org.example.core.models.entities.Entity;
import org.example.core.shared.enums.ItemStatus;
import org.example.core.shared.enums.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public abstract class Item extends Entity {
  private String itemName;
  private BigDecimal startingPrice;
  protected String description;
  private int sellerID;
  protected ItemStatus status;
  private int itemId;

  protected Item() {
    super();
  }

  protected Item(Builder<?> builder) {
    super(builder.createdAt);
    this.itemName = builder.itemName;
    this.startingPrice = builder.startingPrice;
    this.description = builder.description;
    this.sellerID = builder.sellerID;
    this.status = ItemStatus.DRAFT;
  }

  public abstract String getType();

  public abstract static class Builder<T extends Builder<T>> {
    // required
    protected final int sellerID;
    protected final String itemName;
    protected final BigDecimal startingPrice;
    // optional
    protected int itemId;
    protected LocalDateTime createdAt = LocalDateTime.now();
    protected String description;
    protected ItemStatus status;

    protected Builder(int sellerID, String itemName, BigDecimal startingPrice) {
      this.sellerID = sellerID;
      this.itemName = itemName;
      this.startingPrice = startingPrice;
      this.status = ItemStatus.DRAFT;
    }

    protected abstract T self();

    public T id(int id) {
      this.itemId = id;
      return self();
    }

    public T createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return self();
    }

    public T description(String description) {
      this.description = description;
      return self();
    }

    public abstract Item build();
  }

  // getters/setters giữ lại nếu DAO đang cần
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

  public int getSellerID() {
    return sellerID;
  }

  public void setSellerID(int sellerID) {
    this.sellerID = sellerID;
  }

  public ItemStatus getStatus() {
    return status;
  }

  public void setStatus(ItemStatus status) {
    this.status = status;
  }

  public void setItemId(int itemId) {
    this.itemId = itemId;
  }

  public int getItemId() {
    return itemId;
  }
}
