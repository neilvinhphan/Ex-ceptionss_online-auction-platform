package org.example.server.network;

import com.google.gson.*;
import org.example.core.dto.Request;
import org.example.core.models.items.*;
import org.example.core.network.LocalDateTimeAdapter;
import org.example.core.shared.enums.ActionType;
import org.example.server.network.handlers.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bộ xử lý liên kết Client (Session Handler), chịu trách nhiệm duy trì kết nối Socket, đọc/ghi dữ
 * liệu JSON và định tuyến (Routing) các hành động yêu cầu xuống tầng nghiệp vụ Service.
 */
public class ClientHandler implements Runnable {
  private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

  public static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
  public static final ConcurrentHashMap<Integer, ClientHandler> activeUsers = new ConcurrentHashMap<>();

  private final Socket clientSocket;
  private BufferedReader in;
  private PrintWriter out;
  private int currentRoomId = -1;
  private int userId = -1;
  private final Gson gson;

  private final Map<ActionType, RequestHandler> routes = new HashMap<>();

  /** Khởi tạo bộ gom kênh I/O truyền thông cho kết nối Socket Client mới. */
  public ClientHandler(Socket clientSocket) {
    this.clientSocket = clientSocket;
    this.gson = configureGson();
    initializeRoutes();

    try {
      this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      this.out = new PrintWriter(clientSocket.getOutputStream(), true);
      connectedClients.add(this);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Lỗi thiết lập kênh dữ liệu I/O Handler Client", e);
      throw new RuntimeException("Khởi tạo Handler liên kết kết nối thất bại", e);
    }
  }

  private void initializeRoutes() {
    AuthHandler authHandler = new AuthHandler(gson);
    ItemHandler itemHandler = new ItemHandler(gson);
    AuctionHandler auctionHandler = new AuctionHandler(gson);
    FinanceHandler financeHandler = new FinanceHandler(gson);
    UserHandler userHandler = new UserHandler(gson);
    AdminHandler adminHandler = new AdminHandler(gson);

    routes.put(ActionType.REGISTER, authHandler);
    routes.put(ActionType.LOGIN, authHandler);
    routes.put(ActionType.LOGOUT, authHandler);

    routes.put(ActionType.CREATE_ITEM, itemHandler);
    routes.put(ActionType.UPDATE_ITEM_FULL, itemHandler);
    routes.put(ActionType.DELETE_ITEM, itemHandler);
    routes.put(ActionType.GET_PENDING_ITEMS, itemHandler);
    routes.put(ActionType.GET_APPROVED_ITEMS, itemHandler);

    routes.put(ActionType.CREATE_AUCTION, auctionHandler);
    routes.put(ActionType.PLACE_BID, auctionHandler);
    routes.put(ActionType.GET_BID_HISTORY, auctionHandler);
    routes.put(ActionType.GET_ACTIVE_AUCTIONS, auctionHandler);
    routes.put(ActionType.JOIN_ROOM, auctionHandler);
    routes.put(ActionType.LEAVE_ROOM, auctionHandler);
    routes.put(ActionType.REGISTER_AUTOBID, auctionHandler);
    routes.put(ActionType.CANCEL_AUTOBID, auctionHandler);
    routes.put(ActionType.GET_MARKET_HISTORY, auctionHandler);

    routes.put(ActionType.DEPOSIT, financeHandler);
    routes.put(ActionType.GET_PENDING_PAYMENTS, financeHandler);
    routes.put(ActionType.PAY_ITEM, financeHandler);
    routes.put(ActionType.PAY_ALL, financeHandler);
    routes.put(ActionType.GET_PAID_HISTORY, financeHandler);

    routes.put(ActionType.UPDATE_ROLE, userHandler);
    routes.put(ActionType.GET_SELLER_DASHBOARD, userHandler);

    routes.put(ActionType.GET_ADMIN_DASHBOARD_STATS, adminHandler);
    routes.put(ActionType.ADMIN_PROCESS_ITEM, adminHandler);
    routes.put(ActionType.ADMIN_GET_ALL_PENDING_ITEMS, adminHandler);
    routes.put(ActionType.ADMIN_GET_ALL_USERS, adminHandler);
    routes.put(ActionType.ADMIN_BAN_USER, adminHandler);
    routes.put(ActionType.ADMIN_CANCEL_AUCTION, adminHandler);
    routes.put(ActionType.ADMIN_GET_ALL_AUCTIONS, adminHandler);
  }

  /**
   * Vòng lặp liên tục lắng nghe, đón nhận thông điệp chuỗi JSON và phân phối qua bảng định tuyến
   * Actions.
   */
  @Override
  public void run() {
    try {
      String requestJson;
      while ((requestJson = in.readLine()) != null) {
        Request request = gson.fromJson(requestJson, Request.class);
        if (request == null || request.getAction() == null) continue;

        RequestHandler handler = routes.get(request.getAction());
        if (handler != null) {
          handler.handle(request, this);
        } else {
          logger.log(
                  Level.WARNING,
                  "Hành động Action chưa được đăng ký định tuyến: " + request.getAction());
        }
      }
    } catch (IOException e) {
      logger.log(Level.INFO, "Kênh truyền thông với Client đứt gãy đột ngột hoặc đóng luồng");
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi hệ thống phát sinh trong vòng lặp Handler Action", e);
      throw new RuntimeException(e);
    } finally {
      closeConnection();
    }
  }

  /** Đẩy gói tin văn bản JSON xuống hạ tầng mạng tới thiết bị đầu cuối Client. */
  public synchronized void sendMessage(String message) {
    out.println(message);
  }

  /**
   * Phát tán thông báo sảnh (Global Broadcast) tới toàn bộ thiết bị Client đang giữ kết nối TCP.
   */
  public static void broadcastMessage(String message) {
    for (ClientHandler client : connectedClients) {
      try {
        client.sendMessage(message);
      } catch (Exception e) {
        logger.log(Level.WARNING, "Không thể gửi tin sảnh tới một Client Handler nhàn rỗi");
      }
    }
  }

  /** Dọn dẹp tài nguyên và gạch tên Handler khỏi danh sách quản lý khi mất kết nối. */
  private void closeConnection() {
    try {
      if (this.userId != -1) {
        activeUsers.remove(this.userId);
        logger.info("[SERVER] User ID " + this.userId + " đã offline và được xóa khỏi sổ online.");
        this.userId = -1;
      }

      connectedClients.remove(this);
      int oldRoomId = this.currentRoomId;

      if (this.currentRoomId != -1) {
        AuctionServer.removeClientFromRoom(this.currentRoomId, this);
        this.currentRoomId = -1;
      }

      AuctionServer.broadcastRoomUserCount(oldRoomId);
      logger.info(
              "[SERVER] Một Client vừa thoát. Tổng số lượng người dùng kết nối online: "
                      + connectedClients.size());

      if (in != null) in.close();
      if (out != null) out.close();
      if (clientSocket != null) clientSocket.close();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Lỗi phát sinh khi đóng giải phóng hạ tầng Socket Client", e);
    }
  }

  private Gson configureGson() {
    return new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(
                    Item.class,
                    (JsonDeserializer<Item>)
                            (json, typeOfT, context) -> {
                              JsonObject jsonObject = json.getAsJsonObject();
                              String type = jsonObject.get("type").getAsString();
                              switch (type.toUpperCase()) {
                                case "ART" -> {
                                  return context.deserialize(jsonObject, ArtItem.class);
                                }
                                case "ELECTRONICS" -> {
                                  return context.deserialize(jsonObject, ElectronicsItem.class);
                                }
                                case "VEHICLE" -> {
                                  return context.deserialize(jsonObject, VehicleItem.class);
                                }
                                default ->
                                        throw new JsonParseException(
                                                "Không nhận diện được loại tài sản: " + type);
                              }
                            })
            .create();
  }

  public int getUserId() { return userId; }
  public void setUserId(int userId) { this.userId = userId; }
  public int getCurrentRoomId() { return currentRoomId; }
  public void setCurrentRoomId(int currentRoomId) { this.currentRoomId = currentRoomId; }
}