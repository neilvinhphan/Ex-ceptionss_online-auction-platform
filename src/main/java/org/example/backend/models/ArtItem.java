package org.example.backend.models;

import java.time.LocalDateTime;

public class ArtItem extends Item {
  protected String artist;
  protected int creationYear;

  public ArtItem(
      int id,
      LocalDateTime createdAt,
      String type,
      String itemName,
      String artist,
      int creationYear,
      String description,
      double startingPrice) {
    super(id, createdAt, type, itemName, description, startingPrice);
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
    System.out.println("Giá khởi điểm: " + getStartingPrice());
  }
}
