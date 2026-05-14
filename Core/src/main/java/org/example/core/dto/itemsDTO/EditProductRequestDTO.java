package org.example.core.dto.itemsDTO;

import java.math.BigDecimal;

public class EditProductRequestDTO {
  private String itemEditName;
  private String description;
  private BigDecimal price;
  private int itemId;
  private String itemType;

  public EditProductRequestDTO(
      int itemId, String itemEditName, String description, BigDecimal price, String type) {
    this.itemId = itemId;
    this.itemEditName = itemEditName;
    this.description = description;
    this.price = price;
    this.itemType = type;
  }

  public int getItemId() {
    return itemId;
  }

  public void setItemId(int itemId) {
    this.itemId = itemId;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public String getItemEditName() {
    return itemEditName;
  }

  public String getDescription() {
    return description;
  }

  public String getItemType() {
    return itemType;
  }

  public void setItemEditName(String itemEditName) {
    this.itemEditName = itemEditName;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }
}
