package org.example.server.services.authService;

import org.example.core.dto.LoginRequestDTO;
import org.example.core.models.users.User;
import org.example.server.dao.UserDAO;

import static org.example.core.shared.enums.UserStatus.*;

public class AuthLogin {
  private final UserDAO userDAO = new UserDAO();

  public User login(LoginRequestDTO requestPayLoad) throws Exception {

    String nameInCheck = requestPayLoad.getUsername();
    String passInCheck = requestPayLoad.getPassword();

    if (nameInCheck == null || nameInCheck.trim().isEmpty()) {
      throw new Exception("Please enter the username.");
    }

    User userDB = userDAO.getUserInformation(nameInCheck);
    if (userDB.getUserName() == null) {
      throw new Exception("Wrong username or password.");
    }

    //    if (userDB.getStatus() == SUSPENDED) {}
    if (passInCheck == )
  }
}
