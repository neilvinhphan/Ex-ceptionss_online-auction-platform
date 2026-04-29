package org.example.client.utils;

import org.example.core.models.users.User;

public class UserSession {
    private static UserSession instance;
    private User currentUser;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    // Hàm này dùng để xóa thông tin khi người dùng bấm Đăng xuất
    public void cleanUserSession() {
        this.currentUser = null;
    }


}