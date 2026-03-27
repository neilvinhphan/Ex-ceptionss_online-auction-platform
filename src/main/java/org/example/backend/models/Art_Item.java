package org.example.backend.models;

import java.time.LocalDateTime;

public class Art_Item extends Item {
  protected String artist;
  protected int creationYear;

  public Art_Item(
      int id,
      String itemName,
      String artist,
      int creationYear,
      String description,
      LocalDateTime startTime,
      LocalDateTime endTime,
      double startingPrice,
      double currentHightestBid) {
    super(id, itemName, description, startTime, endTime, startingPrice, currentHightestBid);
    this.artist = artist;
    this.creationYear = creationYear;
  }

  public String getArtist() {
    return artist;
  }

  public int getCreationYear() {
    return creationYear;
  }

  public void printInfo() {
    System.out.println(id + ": " + getItemName());
    System.out.println("Mô tả sản phẩm: " + description);
    System.out.println("Thời gian bắt đầu đấu giá: " + startTime);
    System.out.println("Thời gian đóng phiên đấu giá: " + endTime);
    System.out.println("Giá khởi điểm: " + getStartingPrice());
    System.out.println("Giá hiên tại: " + currentHightestBid);
  }
}
