package org.example.server.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;

import org.example.core.dto.*;

import org.example.core.models.entities.Auction;
import org.example.core.shared.enums.AuctionStatus;
import org.example.core.shared.enums.ItemStatus;

import org.example.server.daos.AuctionDAO;
import org.example.server.daos.ItemDAO;

import org.example.core.models.items.Item;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.VehicleItem;
import org.example.core.models.users.User;
import org.example.server.services.AuctionService;
import org.example.server.services.AuthService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.example.core.network.LocalDateTimeAdapter;
import org.example.server.services.ItemService;

public class ClientHandler implements Runnable {
  public static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

  private final Socket clientSocket;
  private BufferedReader in;
  private PrintWriter out;

  // Kéo "bảo bối" TypeAdapter vào để dạy Gson cách đọc Abstract Class Item
  private final Gson gson =
      new GsonBuilder()
          .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
          .registerTypeAdapter(
              Item.class,
              (JsonDeserializer<Item>)
                  (json, typeOfT, context) -> {
                    JsonObject jsonObject = json.getAsJsonObject();
                    String type = jsonObject.get("type").getAsString(); // Đọc xem loại gì
                    switch (type.toUpperCase()) {
                      case "ART":
                        return context.deserialize(jsonObject, ArtItem.class);
                      case "ELECTRONICS":
                        return context.deserialize(jsonObject, ElectronicsItem.class);
                      case "VEHICLE":
                        return context.deserialize(jsonObject, VehicleItem.class);
                      default:
                        throw new JsonParseException("Không nhận diện được loại tài sản: " + type);
                    }
                  })
          .create();

  public ClientHandler(Socket clientSocket) {
    this.clientSocket = clientSocket;
    try {
      this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      this.out = new PrintWriter(clientSocket.getOutputStream(), true);
      // Khách vào thì ghi tên vào sổ (Thêm Client này vào danh sách quản lý)
      connectedClients.add(this);
    } catch (Exception e) {
      throw new RuntimeException("Error initializing client handler: " + e.getMessage(), e);
    }
  }

  @Override
  public void run() {
    try {
      String requestJson;
      while ((requestJson = in.readLine()) != null) {
        Request request = gson.fromJson(requestJson, Request.class);
        if (request != null && request.getAction() != null) {
          switch (request.getAction()) {
            case "REGISTER":
              handleRegister(request);
              break;
            case "LOGIN":
              handleLogin(request);
              break;
            case "CREATE_ITEM":
              handleCreateItem(request);
              break;
            case "GET_PENDING_ITEMS":
              handleGetPendingItems(request);
              break;
            case "CREATE_AUCTION":
              handleCreateAuction(request);
              break;
            case "PLACE_BID":
              handlePlaceBid(request);
              break;
            case "GET_BID_HISTORY":
              handleGetBidHistory(request);
              break;
            case "GET_ACTIVE_AUCTIONS":
              handleGetActiveAuctions(request);
              break;
            default:
              System.out.println("Unknown action: " + request.getAction());
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection();
    }
  }

  private void handleRegister(Request request) {
    try {
      RegisterRequestDTO registerRequest;

      if (request.getData() instanceof RegisterRequestDTO) {
        registerRequest = (RegisterRequestDTO) request.getData();
      } else {
        String dataJson = gson.toJson(request.getData());
        registerRequest = gson.fromJson(dataJson, RegisterRequestDTO.class);
      }

      System.out.println("Registering user: " + registerRequest.getUsername());
      User newUser = AuthService.register(registerRequest);

      Response response;
      if (newUser != null) {
        response = new Response("SUCCESS", "Registration successful");
      } else {
        response = new Response("ERROR", "Registration failed");
      }
      sendMessage(gson.toJson(response));
    } catch (Exception e) {
      e.printStackTrace();
      Response errorRespone = new Response("ERROR", e.getMessage());
      sendMessage(gson.toJson(errorRespone));
    }
  }

  private void handleLogin(Request request) {
    try {
      LoginRequestDTO loginRequest;

      if (request.getData() instanceof LoginRequestDTO) {
        loginRequest = (LoginRequestDTO) request.getData();
      } else {
        String dataJson = gson.toJson(request.getData());
        loginRequest = gson.fromJson(dataJson, LoginRequestDTO.class);
      }
      User newUser = AuthService.login(loginRequest);

      Response response;
      if (newUser != null) {
        response = new Response("SUCCESS", "Login success!", newUser);
      } else {
        response = new Response("ERROR", "Login failed");
      }
      sendMessage(gson.toJson(response));
    } catch (Exception e) {
      e.printStackTrace();
      Response errorRespone = new Response("ERROR", e.getMessage());
      sendMessage(gson.toJson(errorRespone));
    }
  }

  private void handleCreateItem(Request request) {
    try {
      String rawDataJson = gson.toJson(request.getData());
      JsonObject jsonObject = gson.fromJson(rawDataJson, JsonObject.class);
      String type = jsonObject.get("type").getAsString();

      CreateItemRequestDTO finalDTO;

      switch (type.toUpperCase()) {
        case "ART":
          finalDTO = gson.fromJson(rawDataJson, CreateArtItemDTO.class);
          break;
        case "VEHICLE":
          finalDTO = gson.fromJson(rawDataJson, CreateVehicleItemDTO.class);
          break;
        case "ELECTRONICS":
          finalDTO = gson.fromJson(rawDataJson, CreateElectronicsItemDTO.class);
          break;
        default:
          finalDTO = gson.fromJson(rawDataJson, CreateItemRequestDTO.class);
      }

      Item newItem = ItemService.createItem(finalDTO);

      if (newItem != null) {

        Response response = new Response("SUCCESS", "Item created successfully!", newItem);
        sendMessage(gson.toJson(response));
      } else {
        Response errorResponse = new Response("ERROR", "Failed to create item.");
        sendMessage(gson.toJson(errorResponse));
      }

    } catch (Exception e) {
      e.printStackTrace(); // Phải có cái này để soi lỗi ở Console Server!
      // Ép kiểu String rõ ràng để vào đúng constructor message
      Response errorResponse = new Response("ERROR", "Server Error: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleGetPendingItems(Request request) {
    PendingRequestDTO pendingRequest;

    if (request.getData() instanceof PendingRequestDTO) {
      pendingRequest = (PendingRequestDTO) request.getData();
    } else {
      String dataJson = gson.toJson(request.getData());
      pendingRequest = gson.fromJson(dataJson, PendingRequestDTO.class);
    }
    try {
      List<Item> items = ItemService.getAllItem(pendingRequest);
      Response response = new Response("SUCCESS", "Fetched pending items successfully!", items);
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse =
          new Response("ERROR", "Failed to fetch pending items: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleCreateAuction(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());

      // Bây giờ ép kiểu thoải mái, Gson đã tự biết bóc tách Item!
      CreateAuctionDTO auctionReq = gson.fromJson(dataJson, CreateAuctionDTO.class);

      // Gọi Service lưu vào DB
      Auction newAuction = AuctionService.createAuction(auctionReq);

      // Cập nhật trạng thái thành Đang lên sàn
      ItemDAO.getInstance().updateItemStatus(auctionReq.getItem().getItemId(), ItemStatus.LISTED);

      // Báo thành công về Client
      Response response = new Response("SUCCESS", "Đã lên sàn đấu giá thành công!" , newAuction);
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi tạo đấu giá: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handlePlaceBid(Request request) {
    try {
      // Ép kiểu dữ liệu Client gửi lên thành BidRequestDTO
      String dataJson = gson.toJson(request.getData());
      org.example.core.dto.BidRequestDTO bidReq =
          gson.fromJson(dataJson, org.example.core.dto.BidRequestDTO.class);

      // Ném xuống BiddingService để xử lý
      boolean success = org.example.server.services.BiddingService.getInstance().placeBid(bidReq);

      if (success) {
        // Nếu đặt giá hợp lệ, tạo gói tin Broadcast
        String username = "User_" + bidReq.getUserId();

        org.example.core.dto.BidBroadcastDTO broadcastDTO =
            new org.example.core.dto.BidBroadcastDTO(
                bidReq.getAuctionId(),
                bidReq
                    .getBidAmount()
                    .doubleValue(), // Nếu báo đỏ chỗ này thì đổi lại thành BigDecimal
                username);

        // Bọc lại thành Response chuẩn và HÉT LÊN CHO CẢ PHÒNG!
        Response broadcastResponse =
            new Response("NEW_BID", "Có người vừa đặt giá mới", broadcastDTO);
        broadcastMessage(gson.toJson(broadcastResponse));

      } else {
        Response errorResponse = new Response("ERROR_BID", "Đặt giá không thành công.");
        sendMessage(gson.toJson(errorResponse));
      }

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR_BID", e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleGetBidHistory(Request request) {
    try {
      // Giả sử Client gửi data chính là cái auctionId (kiểu int)
      String dataJson = gson.toJson(request.getData());
      Integer auctionId = gson.fromJson(dataJson, Integer.class);

      // Gọi Service lấy danh sách lịch sử
      List<org.example.core.models.entities.BidTransaction> history =
          org.example.server.services.BiddingService.getInstance().getBidHistory(auctionId);

      // Bọc lại thành Response gửi về cho Client
      Response response = new Response("SUCCESS", "Lấy lịch sử thành công", history);
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi lấy lịch sử: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleGetActiveAuctions(Request request) {
    try {
      // Lấy danh sách các sản phẩm từ những phiên đấu giá đang chạy
      List<Auction> activeItems = AuctionDAO.getInstance().getAllAuctionsByStatus(AuctionStatus.RUNNING);

      // Khởi tạo phản hồi thành công
      Response response = new Response("SUCCESS", "Lấy danh sách đấu giá đang diễn ra thành công", activeItems);

      // Gửi về cho client yêu cầu
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi khi lấy danh sách đấu giá: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  public synchronized void sendMessage(String message) {
    out.println(message);
  }

  private void closeConnection() {
    try {
      if (in != null) in.close();
      if (out != null) out.close();
      if (clientSocket != null) clientSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Gửi tin nhắn cho tất cả Client
  public static void broadcastMessage(String message) {
    for (ClientHandler client : connectedClients) {
      client.sendMessage(message);
    }
  }
}
