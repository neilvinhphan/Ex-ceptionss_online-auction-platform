package org.example.backend.models.items;

import org.example.backend.models.Item;

import java.time.LocalDateTime;

public class VehicleItem extends Item {
  protected String brand;
  protected String model;
  protected int manufacturingYear; // Năm sản xuất
  protected double mileage; // số km đã đi

  public VehicleItem(
      int id,
      LocalDateTime createdAt,
      String type,
      String itemName,
      String description,
      double startingPrice) {
    super(id, createdAt, type, itemName, description, startingPrice);
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

  public void printInfo() {
    System.out.println("Mã sản phẩm" + id);
    System.out.println("Xe " + getItemName());
    System.out.println("Thương hiệu: " + brand);
    System.out.println("Năm sản xuất:" + manufacturingYear);
    System.out.println("Số km đã đi: " + mileage);
    System.out.println("Tổng quan về chiếc xe: " + description);
    System.out.println("Giá khởi điểm: " + getStartingPrice());
  }
}
