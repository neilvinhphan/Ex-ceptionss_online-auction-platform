package org.example.core.dto.itemsDTO;

public class CreateVehicleItemDTO extends CreateItemRequestDTO {
  private String brand;
  private String model;
  private int manufacturingYear;
  private double mileage;

  public CreateVehicleItemDTO() {}

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
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
}
