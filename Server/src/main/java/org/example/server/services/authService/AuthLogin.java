package org.example.server.services.authService;

import org.example.core.dto.LoginRequestDTO;
import org.example.core.models.users.User;
import org.example.server.dao.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

import static org.example.core.shared.enums.UserStatus.*;

public class AuthLogin {
  UserDAO userDAO = UserDAO.getInstance();

  public User login(LoginRequestDTO requestPayLoad) throws Exception {

    // Lấy dữ liệu từ giao diện
    String nameInCheck = requestPayLoad.getUsername();
    String passInCheck = requestPayLoad.getPassword();

    // Check rỗng
    if (nameInCheck == null || nameInCheck.trim().isEmpty()) {
      throw new Exception("Please enter the username.");
    }

    // Check username
    User userDB = userDAO.getUserByUsername(nameInCheck);
    if (userDB.getUserName() == null) {
      throw new Exception("Wrong username or password.");
    }

    // Check status
    if (!userDB.getStatus()) {
      throw new Exception("Your accound has banned.");
    }

    // Check password
    if (!(BCrypt.checkpw(passInCheck, userDB.getPassword()))) {
      throw new Exception("Wrong username or password.");
    }
    return userDB;
  }
}
