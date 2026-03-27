package org.example.backend.models;

public class User extends Entity {
  protected String userName;
  protected String phone;
  protected String email;

  public User(int id, String userName, String phone, String email) {
    super(id);
    this.userName = userName;
    this.phone = phone;
    this.email = email;
  }
}
