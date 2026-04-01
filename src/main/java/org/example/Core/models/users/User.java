package org.example.Core.models.users;

import org.example.Core.models.entities.Entity;
import org.example.Core.models.items.ArtItem;
import org.example.Core.models.items.ElectronicsItem;
import org.example.Core.models.items.Item;
import org.example.Core.models.items.VehicleItem;
import org.example.Server.dao.UserDAO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class User extends Entity {
  public static final UserDAO userDAO = new UserDAO();
  protected String userName;
  protected String phone;
  protected String email;
  protected String password;

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
  public User(String userName, String password, String email, String phone) {
    this.userName = userName;
    this.phone = phone;
    this.email = email;
    this.password = password;
  }

  // Lấy dữ liệu từ Database
  public User(int id, String userName, String phone, String email, LocalDateTime createdAt) {
    this.id = id;
    this.createdAt = createdAt;
    this.userName = userName;
    this.phone = phone;
    this.email = email;
  }

  // Constructor rỗng cho DB
  public User() {}

  // SELLER METHODS
  public Item createItem(String type, String name, String desc, BigDecimal startingPrice) {
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

  public void removeItem() {}

  // GETTER & SETTER
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public SellerProfile getSellerProfile() {
    return sellerProfile;
  }

  public void setSellerProfile(SellerProfile sellerProfile) {
    this.sellerProfile = sellerProfile;
  }

  public String getArtist() {
    return artist;
  }

  public void setArtist(String artist) {
    this.artist = artist;
  }

  public int getCreationYear() {
    return creationYear;
  }

  public void setCreationYear(int creationYear) {
    this.creationYear = creationYear;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public int getWarrantyMonths() {
    return warrantyMonths;
  }

  public void setWarrantyMonths(int warrantyMonths) {
    this.warrantyMonths = warrantyMonths;
  }

  public String getCondition() {
    return condition;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }
}
