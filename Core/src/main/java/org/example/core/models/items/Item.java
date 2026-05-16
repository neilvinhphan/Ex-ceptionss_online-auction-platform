package org.example.core.models.items;

import org.example.core.models.entities.Entity;
import org.example.core.shared.enums.ItemStatus;
import org.example.core.shared.enums.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public abstract class Item extends Entity {
  private String itemName;
  protected String description;
  private int sellerID;
  protected ItemStatus status;
  private int itemId;
  private BigDecimal startingPrice;
  private String type;
  private String image;

  protected Item() {
    super();
  }

  protected Item(Builder<?> builder) {
    super(builder.createdAt);
    this.itemName = builder.itemName;
    this.description = builder.description;
    this.sellerID = builder.sellerID;
    this.status = ItemStatus.DRAFT;
    this.startingPrice = builder.startingPrice;
    this.image = builder.image;
  }

  public abstract static class Builder<T extends Builder<T>> {
    // required
    protected final int sellerID;
    protected final String itemName;
    public BigDecimal startingPrice;

    // optional
    protected int itemId;
    protected LocalDateTime createdAt = LocalDateTime.now();
    protected String description;
    protected ItemStatus status;
    protected String image;

    protected Builder(int sellerID, String itemName, BigDecimal startingPrice) {
      this.sellerID = sellerID;
      this.itemName = itemName;
      this.status = ItemStatus.DRAFT;
      this.startingPrice = startingPrice;
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

    public T startingPrice(BigDecimal startingPrice) {
      this.startingPrice = startingPrice;
      return self();
    }

    public abstract Item build();
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
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

  public BigDecimal getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(BigDecimal startingPrice) {
    this.startingPrice = startingPrice;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }
}
