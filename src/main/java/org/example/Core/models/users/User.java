package org.example.core.models.users;

import org.example.core.models.entities.Entity;
import org.example.core.shared.enums.RoleType;
import org.example.core.shared.enums.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class User extends Entity {
  protected int userId;
  protected String userName;
  protected String phone;
  protected String email;
  protected String password;
  protected RoleType role;
  protected UserStatus status;
  private BigDecimal balance;

  // Seller contributes
  private SellerProfile sellerProfile;

  // Đăng ký tài khoản
  public User(String userName, String password, String email, String phone) {
    this.userName = userName;
    this.phone = phone;
    this.email = email;
    this.password = password;
  }

  // Lấy dữ liệu từ Database
  public User(
      String userName,
      String phone,
      String email,
      LocalDateTime createdAt,
      int userId,
      RoleType role,
      BigDecimal balance,
  UserStatus status) {
    this.createdAt = createdAt;
    this.userName = userName;
    this.phone = phone;
    this.email = email;
    this.userId = userId;
    this.role = role;
    this.balance = balance;
    this.status = status;
  }

  // Constructor rỗng cho DB
  public User() {}

  // GETTER & SETTER
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
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

  public RoleType getRole() {
    return role;
  }

  public void setRole(RoleType role) {
    this.role = role;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public UserStatus getStatus() {
    return status;
  }
}
