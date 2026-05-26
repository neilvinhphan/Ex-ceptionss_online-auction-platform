package org.example.core.models.items;

import java.math.BigDecimal;

public class VehicleItem extends Item {
  private String brand;
  private String model;
  private int manufacturingYear;
  private double mileage;

  public VehicleItem() {
    super();
  }

  private VehicleItem(Builder builder) {
    super(builder);
    this.brand = builder.brand;
    this.model = builder.model;
    this.manufacturingYear = builder.manufacturingYear;
    this.mileage = builder.mileage;
  }

  public static class Builder extends Item.Builder<Builder> {
    private String brand;
    private String model;
    private int manufacturingYear;
    private double mileage;

    public Builder(int sellerID, String itemName, BigDecimal startingPrice) {
      super(sellerID, itemName, startingPrice);
    }

    public Builder brand(String brand) {
      this.brand = brand;
      return this;
    }

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder manufacturingYear(int manufacturingYear) {
      this.manufacturingYear = manufacturingYear;
      return this;
    }

    public Builder mileage(double mileage) {
      this.mileage = mileage;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public VehicleItem build() {
      return new VehicleItem(this);
    }
  }

  // GETTER & SETTER
  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public int getManufacturingYear() {
    return manufacturingYear;
  }

  public void setManufacturingYear(int manufacturingYear) {
    this.manufacturingYear = manufacturingYear;
  }

  public double getMileage() {
    return mileage;
  }

  public void setMileage(double mileage) {
    this.mileage = mileage;
  }

  @Override
  public String getType() {
    return "VEHICLE";
  }
}
