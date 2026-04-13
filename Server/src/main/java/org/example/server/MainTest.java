package org.example.server;

import org.example.core.dto.LoginRequestDTO;
import org.example.core.dto.RegisterRequestDTO;
import org.example.core.models.users.User;
import org.example.server.services.authService.AuthLogin;
import org.example.server.services.authService.AuthRegister;

public class MainTest {
  public static void main(String[] args) {

    AuthRegister newtest1 = new AuthRegister();
    AuthLogin newtest2 = new AuthLogin();

    System.out.println("=== 🚀 BẮT ĐẦU CHẠY THỬ NGHIỆM AUTH SERVICE ===");

    // ---------------------------------------------------------
    // KỊCH BẢN 1: TEST ĐĂNG KÝ
    // ---------------------------------------------------------
    System.out.println("\n[1] Đang test luồng ĐĂNG KÝ...");
    try {
      // My đóng gói DTO
      RegisterRequestDTO regReq =
          new RegisterRequestDTO("vinh_techlead", "matkhau123", "vinh@uet.edu.vn", "0987654321");

      // Bắn vào Service của bro
      User newUser = newtest1.register(regReq);

      System.out.println("✅ KẾT QUẢ: Đăng ký THÀNH CÔNG!");
      System.out.println("👉 Username: " + newUser.getUserName());
      System.out.println("👉 Mật khẩu đã băm (Sẽ lưu vào DB): " + newUser.getPassword());
    } catch (Exception e) {
      System.err.println("❌ THẤT BẠI: " + e.getMessage());
    }

    // ---------------------------------------------------------
    // KỊCH BẢN 2: TEST ĐĂNG NHẬP (Mọi thứ hoàn hảo)
    // ---------------------------------------------------------
    System.out.println("\n[2] Đang test luồng ĐĂNG NHẬP (Đúng Pass)...");
    try {
      LoginRequestDTO loginReq = new LoginRequestDTO("vinh_techlead", "matkhau123");

      User loggedInUser = newtest2.login(loginReq);

      System.out.println(
          "✅ KẾT QUẢ: Đăng nhập THÀNH CÔNG! Chào mừng sếp " + loggedInUser.getUserName());
    } catch (Exception e) {
      System.err.println("❌ THẤT BẠI: " + e.getMessage());
    }

    // ---------------------------------------------------------
    // KỊCH BẢN 3: TEST "GÁC ĐỀN" (Cố tình nhập sai Pass)
    // ---------------------------------------------------------
    System.out.println("\n[3] Đang test luồng HACKER (Sai Pass)...");
    try {
      LoginRequestDTO hackerReq = new LoginRequestDTO("vinh_techlead", "111111");
      newtest2.login(hackerReq);

      // Nếu code chạy được đến dòng này tức là Service của bro đang bị LỖI (không chịu chặn pass
      // sai)
      System.out.println(
          "🚨 BÁO ĐỘNG: Ủa sao sai pass mà vẫn vào được??? Kiểm tra lại hàm login ngay!");
    } catch (Exception e) {
      // Nếu nhảy vào catch này chứng tỏ bro đã throw Exception rất chuẩn
      System.out.println("✅ KẾT QUẢ: Chặn thành công! Lời chửi từ Service: " + e.getMessage());
    }
  }
}
