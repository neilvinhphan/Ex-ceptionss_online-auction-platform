package org.example.server.services.authservice;

import org.example.core.models.users.User;
import org.example.server.dao.UserDAO;

import java.util.Scanner;

public class UpgradeToSeller {
    public static void upgradetoSeller() {
        Scanner input = new Scanner(System.in);
        User user = new User();
        user.setUserName(input.nextLine());
        try{UserDAO userDAO = UserDAO.getInstance();
            if (!userDAO.isSeller(user.getUserName())) {
                userDAO.upgradeToSeller(user.getUserName());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
