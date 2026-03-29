package org.example.backend.models;

import org.example.backend.models.items.ArtItem;
import org.example.backend.models.items.ElectronicsItem;
import org.example.backend.models.items.VehicleItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class User extends Entity {
  protected String userName;
  protected String phone;
  protected String email;

  // Seller contributes
  private SellerProfile sellerProfile;
  protected String artist;
  protected int creationYear;
  protected String brand;
  protected int warrantyMonths;
  protected String condition;

  // Bidder contributes
  private BigDecimal balance;

  // Đăng ký tài khoản
  private User(String userName, String phone, String email) {
    this.userName = userName;
    this.phone = phone;
    this.email = email;
  }

  // Lấy dữ liệu từ Database
  public User(int id, String userName, String phone, String email, LocalDateTime createdAt) {
    this.id = id;
    this.createdAt = createdAt;
    this.userName = userName;
    this.phone = phone;
    this.email = email;
  }

  // SELLER METHODS
  public Item createItem(String type, String name, String desc, double startingPrice) {
    Item newItem = null;
    if (type.equalsIgnoreCase("Vehicle")) {
      newItem = new VehicleItem(0, null, type, name, desc, startingPrice);
    } else if (type.equalsIgnoreCase("Art")) {
      newItem = new ArtItem(0, null, type, name, artist, creationYear, desc, startingPrice);
    } else if (type.equalsIgnoreCase("Electronics")) {
      newItem =
          new ElectronicsItem(
              0, null, type, name, brand, warrantyMonths, condition, desc, startingPrice);
    }
    return newItem;
  }

  public void updateItem(
      Item needChange, String newType, String newName, String newDesc, double newStartingPrice) {
    needChange.setType(newType);
    needChange.setItemName(newName);
    needChange.setDescription(newDesc);
  }

  public void removeItem() {}

  // BIDDER METHODS
  public boolean bid() {
    return true;
  }

  //  public List<BidTransaction> getBidHistory(Auction auction) {return...}
}
