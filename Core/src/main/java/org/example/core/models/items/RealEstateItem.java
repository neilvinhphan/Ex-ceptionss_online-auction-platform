package org.example.core.models.items;

import java.math.BigDecimal;

public class RealEstateItem extends Item {
  private String location; // Vị trí/Địa chỉ
  private double area; // Diện tích (m2)
  private String propertyType; // Loại hình (Đất nền, Chung cư...)
  private String legalStatus; // Tình trạng pháp lý (Sổ đỏ, HĐMB...)

  public RealEstateItem() {
    super();
  }

  private RealEstateItem(Builder builder) {
    super(builder);
    this.location = builder.location;
    this.area = builder.area;
    this.propertyType = builder.propertyType;
    this.legalStatus = builder.legalStatus;
  }

  public static class Builder extends Item.Builder<Builder> {
    private String location;
    private double area;
    private String propertyType;
    private String legalStatus;

    public Builder(int sellerID, String itemName, BigDecimal startingPrice) {
      super(sellerID, itemName, startingPrice);
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    public Builder area(double area) {
      this.area = area;
      return this;
    }

    public Builder propertyType(String propertyType) {
      this.propertyType = propertyType;
      return this;
    }

    public Builder legalStatus(String legalStatus) {
      this.legalStatus = legalStatus;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public RealEstateItem build() {
      return new RealEstateItem(this);
    }
  }

  public String getLocation() {
    return location;
  }

  public double getArea() {
    return area;
  }

  public String getPropertyType() {
    return propertyType;
  }

  public String getLegalStatus() {
    return legalStatus;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public void setArea(double area) {
    this.area = area;
  }

  public void setPropertyType(String propertyType) {
    this.propertyType = propertyType;
  }

  public void setLegalStatus(String legalStatus) {
    this.legalStatus = legalStatus;
  }

  @Override
  public String getType() {
    return "REALESTATE";
  }
}
