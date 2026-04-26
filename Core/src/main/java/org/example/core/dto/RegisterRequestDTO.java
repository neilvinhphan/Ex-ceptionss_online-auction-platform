package org.example.core.dto;

public class RegisterRequestDTO {
  private String username;
  private String phone;
  private String email;
  private String password;
  private String rePassword;
  private String passwordHide;
  private String rePasswordHide;
  private boolean tickCheck;

  public RegisterRequestDTO(
      String username,
      String phone,
      String email,
      String password,
      String rePassword,
      String passwordHide,
      String rePasswordHide,
      boolean tickCheck) {
    this.username = username;
    this.phone = phone;
    this.email = email;
    this.password = password;
    this.rePassword = rePassword;
    this.passwordHide = passwordHide;
    this.rePasswordHide = rePasswordHide;
    this.tickCheck = tickCheck;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
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

  public String getRePasswordHide() {
    return rePasswordHide;
  }

  public void setRePasswordHide(String rePasswordHide) {
    this.rePasswordHide = rePasswordHide;
  }

  public String getPasswordHide() {
    return passwordHide;
  }

  public void setPasswordHide(String passwordHide) {
    this.passwordHide = passwordHide;
  }

  public String getRePassword() {
    return rePassword;
  }

  public void setRePassword(String rePassword) {
    this.rePassword = rePassword;
  }

  public boolean isTickCheck() {
    return tickCheck;
  }

  public void setTickCheck(boolean tickCheck) {
    this.tickCheck = tickCheck;
  }
}
