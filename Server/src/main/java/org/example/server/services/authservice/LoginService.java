package org.example.server.services.authservice;

import org.example.core.models.users.User;
import org.example.server.dao.UserDAO;

public class LoginService {
    public static void main(String[] args){
        String username = "user1";
        String password = "123456789";
        try {UserDAO userDAO = UserDAO.getInstance();
            User user = userDAO.checkLogin(username,password);
            if (user != null) {
                System.out.println(user.getUserName());
                System.out.println(user.getBalance());
            }
    } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
