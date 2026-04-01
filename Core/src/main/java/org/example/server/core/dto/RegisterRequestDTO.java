package org.example.server.core.dto;

public class RegisterRequestDTO {
  public String firstname;
  public String lastname;
  public String middlename;
  public String username;
  public String phone;
  public String email;
  public String password;

  public RegisterRequestDTO(
      String firstname,
      String lastname,
      String middlename,
      String username,
      String phone,
      String email,
      String password) {
    this.firstname = firstname;
    this.lastname = lastname;
    this.middlename = middlename;
    this.username = username;
    this.phone = phone;
    this.email = email;
    this.password = password;
  }

  public String getFirstname() {
    return firstname;
  }

  public void setFirstname(String firstname) {
    this.firstname = firstname;
  }

  public String getLastname() {
    return lastname;
  }

  public void setLastname(String lastname) {
    this.lastname = lastname;
  }

  public String getMiddlename() {
    return middlename;
  }

  public void setMiddlename(String middlename) {
    this.middlename = middlename;
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
}
