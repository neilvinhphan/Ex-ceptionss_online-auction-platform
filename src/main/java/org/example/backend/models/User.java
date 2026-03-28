package org.example.backend.models;

import java.time.LocalDateTime;

public class User extends Entity {
  protected String userName;
  protected String phone;
  protected String email;

  // Đăng ký tài khoản
  public User(String userName, String phone, String email) {
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
}
