package org.example.server;

import java.io.InputStream;
import java.util.logging.LogManager;

import org.example.server.config.DBConnection;
import org.example.server.network.AuctionServer;
import org.example.server.services.AuctionService;

public class ServerMain {
  static void main() {
    try (InputStream is = ServerMain.class.getClassLoader().getResourceAsStream("logging.properties")) {
      if (is != null) {
        LogManager.getLogManager().readConfiguration(is);
        System.err.println("[LOGGER] Đã áp dụng cấu hình từ logging.properties!");
      } else {
        System.err.println("⚠[LOGGER] Không tìm thấy file logging.properties, dùng cấu hình mặc định.");
      }
    } catch (Exception e) {
      System.err.println("[LOGGER] Lỗi khi nạp cấu hình log: " + e.getMessage());
    }
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.err.println("\n[SHUTDOWN] Phát hiện tín hiệu dừng Server. Đang dọn dẹp hệ thống...");

      DBConnection.shutdown();

      System.err.println("[SHUTDOWN] Hệ thống đã dừng an toàn hoàn toàn.");
    }));
    AuctionServer server = new AuctionServer();
    System.err.println("Đang nạp lại tiến trình đấu giá ngầm...");
    AuctionService.getInstance().reloadScheduledTasksOnStartup();
    server.start();
  }
}