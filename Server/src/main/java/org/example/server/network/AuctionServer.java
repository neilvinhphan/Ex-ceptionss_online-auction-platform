package org.example.server.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.example.core.dto.Response;
import org.example.core.network.LocalDateTimeAdapter;
import org.example.server.services.AuctionService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {
  private static final int PORT = 9000;
  private final ExecutorService executor = Executors.newFixedThreadPool(50);
  private final Gson gson = new Gson();

  private static final Gson customGson = new GsonBuilder()
          .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
          .create();
  /**
   * TRẠM ĐIỀU PHỐI PHÒNG: Lưu trữ danh sách Client theo từng AuctionId. Dùng ConcurrentHashMap để
   * đảm bảo an toàn khi nhiều luồng cùng truy cập (Thread-safe).
   */
  public static final ConcurrentHashMap<Integer, List<ClientHandler>> roomClients =
      new ConcurrentHashMap<>();

  public void start() {
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      System.out.println("🚀 Auction Server started on port " + PORT);

      // Gỡ bỏ AutoCloseJob cũ.
      // Lưu ý: Việc gọi reloadScheduledTasksOnStartup() nên để ở ServerMain
      // hoặc gọi ngay tại đây trước khi vào vòng lặp while(true).

      while (true) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("📡 New client connected: " + clientSocket.getInetAddress());

        ClientHandler handler = new ClientHandler(clientSocket);
        executor.execute(handler);
      }
    } catch (IOException e) {
      throw new RuntimeException("❌ Error starting server: " + e.getMessage(), e);
    }
  }

  /**
   * HÀM PHÁT LOA (BROADCAST): Bắn thông báo đến tất cả User trong 1 phòng cụ thể. Được gọi bởi
   * AuctionService khi trạng thái phòng thay đổi (Mở/Đóng/Có Bid mới).
   */
  public static void broadcastToRoom(int auctionId, Response response) {
    List<ClientHandler> clientsInRoom = roomClients.get(auctionId);

    if (clientsInRoom != null && !clientsInRoom.isEmpty()) {
      String jsonMsg = customGson.toJson(response);

      // Duyệt qua danh sách Client trong phòng và gửi tin nhắn
      // Dùng đồng bộ hóa nhẹ để tránh lỗi khi có người thoát phòng giữa chừng
      synchronized (clientsInRoom) {
        clientsInRoom.removeIf(
            client -> {
              try {
                client.sendMessage(jsonMsg);
                return false;
              } catch (Exception e) {
                // Nếu gửi lỗi (client đã sập), tự động xóa khỏi phòng luôn
                return true;
              }
            });
      }
      System.out.println(
          "📢 Broadcasted ["
              + response.getStatus()
              + "] to "
              + clientsInRoom.size()
              + " users in Room "
              + auctionId);
    }
  }


  public static void addClientToRoom(int auctionId, ClientHandler client) {
    roomClients.computeIfAbsent(auctionId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
            .add(client);
    System.out.println("👥 [SERVER ROOM] Client " + client.hashCode() + " gia nhập phòng " + auctionId);
  }

  public static void removeClientFromRoom(int auctionId, ClientHandler client) {
    java.util.List<ClientHandler> clients = roomClients.get(auctionId);
    if (clients != null) {
      clients.remove(client);
      System.out.println("👥 [SERVER ROOM] Client " + client.hashCode() + " rời khỏi phòng " + auctionId);
      if (clients.isEmpty()) {
        roomClients.remove(auctionId); // Phòng không còn ai thì giải phóng luôn
      }
    }
  }

  public static void broadcastRoomUserCount(int auctionId) {
    if (auctionId == -1) return;
    java.util.List<ClientHandler> clients = roomClients.get(auctionId);
    int count = (clients != null) ? clients.size() : 0;

    // Đóng gói gói tin Response mang nhãn ROOM_USER_COUNT, ném số lượng vào message
    Response countResponse = new Response("ROOM_USER_COUNT", String.valueOf(count));
    broadcastToRoom(auctionId, countResponse);
  }
}
