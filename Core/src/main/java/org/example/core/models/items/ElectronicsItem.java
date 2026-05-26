package org.example.core.models.items;

import java.math.BigDecimal;

public class ElectronicsItem extends Item {
  private String brand;
  private int warrantyMonths;
  private String condition;

  public ElectronicsItem() {
    super();
  }

  private ElectronicsItem(Builder builder) {
    super(builder);
    this.brand = builder.brand;
    this.warrantyMonths = builder.warrantyMonths;
    this.condition = builder.condition;
  }

  public static class Builder extends Item.Builder<Builder> {
    private String brand;
    private int warrantyMonths;
    private String condition;

    public Builder(int sellerID, String itemName, BigDecimal startingPrice) {
      super(sellerID, itemName, startingPrice);
    }

    public Builder brand(String brand) {
      this.brand = brand;
      return this;
    }

    public Builder warrantyMonths(int warrantyMonths) {
      this.warrantyMonths = warrantyMonths;
      return this;
    }

    public Builder condition(String condition) {
      this.condition = condition;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public ElectronicsItem build() {
      return new ElectronicsItem(this);
    }
  }

  public String getBrand() {
    return brand;
  }

  public int getWarrantyMonths() {
    return warrantyMonths;
  }

  public String getCondition() {
    return condition;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public void setWarrantyMonths(int warrantyMonths) {
    this.warrantyMonths = warrantyMonths;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  @Override
  public String getType() {
    return "ELECTRONICS";
  }
}
