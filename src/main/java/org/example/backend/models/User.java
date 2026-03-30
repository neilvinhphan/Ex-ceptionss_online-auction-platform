package org.example.backend.models;

import org.example.backend.models.items.ArtItem;
import org.example.backend.models.items.ElectronicsItem;
import org.example.backend.models.items.VehicleItem;
import org.example.database.UserDAO;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

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

  // Bổ sung code
  public User() {}

  public String getUsername() {
    return userName;
  }

  public String getPassword() {
    return password;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  static void main() throws SQLException {
    User user = userDAO.getUserInformation("klbc_0211");
    System.out.println(user.getUsername() + " " + user.getBalance());
  }
}
