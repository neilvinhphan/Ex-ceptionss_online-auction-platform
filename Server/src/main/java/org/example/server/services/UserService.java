package org.example.server.services;

import org.example.core.models.users.SellerProfile;
import org.example.core.models.users.User;
import org.example.server.dao.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

public class UserService {
  private final UserDAO userDAO;

  // SỬA: Thêm Constructor Injection để hỗ trợ Unit Test
  public UserService(UserDAO userDAO) {
    this.userDAO = userDAO;
  }

  // Default constructor giữ lại để code cũ của em không bị lỗi
  public UserService() {
    this.userDAO = UserDAO.getInstance();
  }

  // Xem thông tin
  public User viewProfile(String username) throws Exception {
    if (username == null || username.trim().isEmpty()) {
      throw new Exception("Username is required.");
    }

    User user = userDAO.getUserByUsername(username);
    if (user == null || user.getUserName() == null) {
      throw new Exception("User not found.");
    }
    return user;
  }

  // Đổi mật khẩu
  public void changePassword(String username, String currentPassword, String newPassword)
      throws Exception {
    if (username == null || username.trim().isEmpty()) {
      throw new Exception("Username is required.");
    }
    if (currentPassword == null || currentPassword.trim().isEmpty()) {
      throw new Exception("Current password is required.");
    }
    if (newPassword == null || newPassword.trim().isEmpty()) {
      throw new Exception("New password is required.");
    }
    if (newPassword.length() < 6) {
      throw new Exception("New password must contain at least 6 characters.");
    }

    User user = userDAO.getUserByUsername(username);
    if (user == null || user.getUserName() == null) {
      throw new Exception("User not found.");
    }
    if (!BCrypt.checkpw(currentPassword, user.getPassword())) {
      throw new Exception("Current password is incorrect.");
    }
    if (BCrypt.checkpw(newPassword, user.getPassword())) {
      throw new Exception("New password must be different from current password.");
    }

    String hashedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
    boolean success = userDAO.updatePassword(user.getId(), hashedNewPassword);
    if (!success) {
      throw new Exception("Cannot change password.");
    }
  }

  // Cập nhật rating
  public User updateSellerRating(String username, double rating) throws Exception {
    if (username == null || username.trim().isEmpty()) {
      throw new Exception("Username is required.");
    }
    if (rating < 0 || rating > 5) {
      throw new Exception("Rating must be in range 0 to 5.");
    }

    User user = userDAO.getUserByUsername(username);
    if (user == null || user.getUserName() == null) {
      throw new Exception("User not found.");
    }

    if (user.getSellerProfile() == null) {
      user.setSellerProfile(new SellerProfile());
    }
    user.getSellerProfile().setRating(rating);

    boolean success = userDAO.updateRatingByUsername(username, rating);
    if (!success) {
      throw new Exception("Cannot update seller rating.");
    }
    return user;
  }
}
