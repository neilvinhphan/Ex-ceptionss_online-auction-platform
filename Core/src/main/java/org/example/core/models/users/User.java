package org.example.core.models.users;

import org.example.core.models.entities.Entity;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class User extends Entity {
  protected String userName;
  protected String phone;
  protected String email;
  protected String password;
  protected String role;
  protected boolean status;

  // Seller contributes
  private SellerProfile sellerProfile;

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
  //  public Item createItem(String type, String name, String desc, BigDecimal startingPrice) {
  //    Item newItem = null;
  //    if (type.equalsIgnoreCase("Vehicle")) {
  //      newItem = new VehicleItem(0, null, type, name, desc, startingPrice);
  //    } else if (type.equalsIgnoreCase("Art")) {
  //      newItem = new ArtItem(0, null, type, name, artist, creationYear, desc, startingPrice);
  //    } else if (type.equalsIgnoreCase("Electronics")) {
  //      newItem =
  //          new ElectronicsItem(
  //              0, null, type, name, brand, warrantyMonths, condition, desc, startingPrice);
  //    }
  //    return newItem;
  //  }

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

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public void setStatus(boolean status) {
    this.status = status;
  }

  public boolean getStatus() {
    return status;
  }
}
