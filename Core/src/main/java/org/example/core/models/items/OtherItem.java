package org.example.core.models.items;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OtherItem extends Item {
  protected String category; // Phân loại (Ví dụ: Nội thất, Quần áo, Thẻ bài sưu tầm...)
  protected String origin; // Xuất xứ
  protected double weight; // Trọng lượng (kg)

  public OtherItem(
      int sellerID,
      LocalDateTime createdAt,
      String type,
      String itemName,
      String category,
      String origin,
      double weight,
      String description,
      BigDecimal startingPrice) {
    super(sellerID, createdAt, type, itemName, description, startingPrice);
    this.category = category;
    this.origin = origin;
    this.weight = weight;
  }
  public OtherItem() {}

  public String getCategory() {
    return category;
  }

  public String getOrigin() {
    return origin;
  }

  public double getWeight() {
    return weight;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public void setWeight(double weight) {
    this.weight = weight;
  }

  public void printInfo() {
    System.out.println("Mã sản phẩm: " + id);
    System.out.println("Sản phẩm khác: " + getItemName());
    System.out.println("Phân loại: " + category);
    System.out.println("Xuất xứ: " + origin);
    System.out.println("Trọng lượng: " + weight + " kg");
    System.out.println("Mô tả chi tiết: " + description);
    ;
    System.out.println("Giá khởi điểm: " + getStartingPrice());
  }
}
