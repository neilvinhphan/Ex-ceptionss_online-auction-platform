package org.example.server.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.core.dto.Response;
import org.example.core.network.LocalDateTimeAdapter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Máy chủ trung tâm điều phối kết nối Socket TCP, quản lý luồng real-time
 * và thực hiện nghiệp vụ broadcast theo từng phòng đấu giá.
 */
public class AuctionServer {
  private static final Logger logger = Logger.getLogger(AuctionServer.class.getName());
  private static final int PORT = 9000;

  private final ExecutorService executor = Executors.newFixedThreadPool(50);
  private final Gson gson = new Gson();

  private static final Gson customGson = new GsonBuilder()
          .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
          .create();

  /**
   * Trạm điều phối phòng: Lưu trữ danh sách ClientHandler hoạt động thực tế theo từng mã phiên đấu giá.
   */
  public static final ConcurrentHashMap<Integer, List<ClientHandler>> roomClients =
          new ConcurrentHashMap<>();

  /**
   * Khởi chạy máy chủ Socket, mở cổng lắng nghe và phân phối các kết nối Client vào Thread Pool.
   */
  public void start() {
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      logger.info("[AUCTION SERVER] Started and listening on port " + PORT);

      while (true) {
        Socket clientSocket = serverSocket.accept();
        logger.info("[CONNECTION] New client connected from: " + clientSocket.getInetAddress());

        ClientHandler handler = new ClientHandler(clientSocket);
        executor.execute(handler);
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Lỗi nghiêm trọng khi vận hành máy chủ Socket", e);
      throw new RuntimeException("Không thể khởi động Auction Server: " + e.getMessage(), e);
    }
  }

  /**
   * Phát loa toàn phòng (Broadcast): Gửi gói tin phản hồi đến toàn bộ người dùng đang có mặt trong phòng đấu giá cụ thể.
   * Đồng thời tự động dọn dẹp các liên kết Handler đã sập mạng ngầm.
   */
  public static void broadcastToRoom(int auctionId, Response response) {
    List<ClientHandler> clientsInRoom = roomClients.get(auctionId);

    if (clientsInRoom != null && !clientsInRoom.isEmpty()) {
      String jsonMsg = customGson.toJson(response);

      synchronized (clientsInRoom) {
        clientsInRoom.removeIf(client -> {
          try {
            client.sendMessage(jsonMsg);
            return false;
          } catch (Exception e) {
            logger.log(Level.WARNING, "Gửi tin tới Client thất bại, tự động ngắt kết nối khỏi phòng " + auctionId);
            return true;
          }
        });
      }
      logger.info("Broadcasted [" + response.getStatus() + "] to " + clientsInRoom.size() + " users in Room " + auctionId);
    }
  }

  /**
   * Ghi danh bổ sung một kết nối Handler vào danh sách bộ nhớ đệm của phòng đấu giá.
   */
  public static void addClientToRoom(int auctionId, ClientHandler client) {
    roomClients.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>())
            .add(client);
    logger.info("👥 [SERVER ROOM] Client " + client.hashCode() + " gia nhập phòng " + auctionId);
  }

  /**
   * Trích xuất loại bỏ một Handler khỏi phòng đấu giá, giải phóng tài nguyên phòng nếu trống rỗng.
   */
  public static void removeClientFromRoom(int auctionId, ClientHandler client) {
    List<ClientHandler> clients = roomClients.get(auctionId);
    if (clients != null) {
      clients.remove(client);
      logger.info("👥 [SERVER ROOM] Client " + client.hashCode() + " rời khỏi phòng " + auctionId);
      if (clients.isEmpty()) {
        roomClients.remove(auctionId);
      }
    }
  }

  /**
   * Phát tán cập nhật số lượng người xem real-time hiện tại cho toàn bộ Client bên trong phòng gác.
   */
  public static void broadcastRoomUserCount(int auctionId) {
    if (auctionId == -1) return;
    List<ClientHandler> clients = roomClients.get(auctionId);
    int count = (clients != null) ? clients.size() : 0;

    Response countResponse = new Response("ROOM_USER_COUNT", String.valueOf(count));
    broadcastToRoom(auctionId, countResponse);
  }
}