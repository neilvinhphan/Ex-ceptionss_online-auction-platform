package org.example.core.models.items;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AntiqueItem extends Item {
  protected String era; // niên đại<xuất xứ>
  protected String material;
  protected String condition; // Tình trạng
  protected boolean isCertified;

  public AntiqueItem(
      int id,
      LocalDateTime createdAt,
      String type,
      String itemName,
      String era,
      String material,
      String condition,
      boolean isCertified,
      String description,
      BigDecimal startingPrice) {
    super(id, createdAt, type, itemName, description, startingPrice);
    this.era = era;
    this.material = material;
    this.condition = condition;
    this.isCertified = isCertified;
  }

  public String getEra() {
    return era;
  }

  public String getMaterial() {
    return material;
  }

  public String getCondition() {
    return condition;
  }

  public void setEra(String era) {
    this.era = era;
  }

  public void setMaterial(String material) {
    this.material = material;
  }

  public void setCertified(boolean certified) {
    isCertified = certified;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  public boolean isCertified() {
    return isCertified;
  }

  @Override
  public void printInfo() {
    System.out.println("Mã sản phẩm: " + id);
    System.out.println("Đồ cổ: " + getItemName());
    System.out.println("Niên đại/Xuất xứ: " + era);
    System.out.println("Chất liệu: " + material);
    System.out.println("Tình trạng bảo quản: " + condition);
    System.out.println(
        "Giấy thẩm định: " + (isCertified ? "Đã có chứng nhận chuyên gia" : "Chưa qua thẩm định"));
    System.out.println("Mô tả chi tiết: " + description);
    System.out.println("Giá khởi điểm: " + getStartingPrice());
  }
}
