package org.example.server.services.authservice;

import org.example.core.models.users.User;
import org.example.server.dao.UserDAO;

import java.util.Scanner;

public class RegisterService {
  static void main() {
    Scanner input = new Scanner(System.in);
    try {
      UserDAO userDAO = UserDAO.getInstance();
      User user = new User();
      user.setUserName(input.nextLine());
      user.setPassword(input.nextLine());
      user.setPhone(input.nextLine());
      user.setEmail(input.nextLine());
      userDAO.registerUser(user);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
