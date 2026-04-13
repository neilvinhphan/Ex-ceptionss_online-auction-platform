package org.example.core.models.items;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ElectronicsItem extends Item {
  protected String brand;
  protected int warrantyMonths; // Số tháng bảo hành
  protected String condition; // tình trạng của sản phẩm

  public ElectronicsItem(
      int id,
      LocalDateTime createdAt,
      String type,
      String itemName,
      String brand,
      int warrantyMonths,
      String condition,
      String description,
      BigDecimal startingPrice) {
    super(id, createdAt, type, itemName, description, startingPrice);
    this.brand = brand;
    this.warrantyMonths = warrantyMonths;
    this.condition = condition;
  }
  public ElectronicsItem() {}

  public String getCondition() {
    return condition;
  }

  public String getBrand() {
    return brand;
  }

  public int getWarrantyMonths() {
    return warrantyMonths;
  }

  public void printInfo() {
    System.out.println("Mã sản phẩm" + id);
    System.out.println("Xe " + getItemName());
    System.out.println("Thương hiệu: " + brand);
    System.out.println("Số tháng bảo hành sản phẩm " + warrantyMonths);
    System.out.println("Tình trạng sản phẩm: " + condition);
    System.out.println("Mô tả sản phẩm: " + description);
    System.out.println("Giá khởi điểm: " + getStartingPrice());
  }
}
