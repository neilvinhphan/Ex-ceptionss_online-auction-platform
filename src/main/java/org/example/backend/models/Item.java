package org.example.backend.models;

import java.time.LocalDateTime;

public abstract class Item extends Entity {
  private final String itemName;
  private double startingPrice;
  protected String description; // Mô tả chi tiết
  protected LocalDateTime startTime;
  protected LocalDateTime endTime;
  protected double currentHightestBid; // giá cao nhất hiện tại

  public Item(
      int id,
      String itemName,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime,
      double startingPrice,
      double currentHightestBid) {
    super(id);
    this.itemName = itemName;
    this.startingPrice = startingPrice;
    this.description = description;
    this.startTime = startTime;
    this.endTime = endTime;
    this.currentHightestBid = currentHightestBid;
  }

  // chỉ set giá cao nhất để có thể điều chỉnh khi đấu giá
  public void setCurrentHightestBid(double currentHightestBid) {
    this.currentHightestBid = currentHightestBid;
  }

  // get tất cả các thuộc tính
  public String getItemName() {
    return itemName;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  public String getDescription() {
    return description;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public double getCurrentHightestBid() {
    return currentHightestBid;
  }

  abstract void printInfo();
  /*
  Là một "Bản hợp đồng" (Contract): Khi bạn khai báo một phương thức là abstract trong lớp cha Item,
  phương thức đó sẽ không có phần thân code (không có cặp ngoặc nhọn {}).
  Nó mang ý nghĩa: "Tôi là một Sản phẩm (Item), tôi chắc chắn có thể in ra thông tin của mình (printInfo),
  nhưng in ra cụ thể như thế nào thì các lớp con tự đi mà định nghĩa".

  Sự bắt buộc (Enforcement): Bất kỳ lớp con nào kế thừa từ Item (như Art, Vehicle, Electronics) bắt buộc phải ghi đè
  (override) phương thức này và viết code chi tiết cho nó. Nếu không ghi đè, trình biên dịch Java sẽ báo lỗi ngay lập tức.
   */
}
