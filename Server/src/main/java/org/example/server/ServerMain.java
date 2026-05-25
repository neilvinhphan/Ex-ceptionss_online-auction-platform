package org.example.server;

import org.example.server.network.AuctionServer;
import org.example.server.services.AuctionService;

public class ServerMain {
  static void main() {
    AuctionServer server = new AuctionServer();
    // Khôi phục các tiến trình hẹn giờ nếu server vừa bị restart
    System.out.println("Đang nạp lại tiến trình đấu giá ngầm...");
    AuctionService.getInstance().reloadScheduledTasksOnStartup();
    server.start();
  }
}
