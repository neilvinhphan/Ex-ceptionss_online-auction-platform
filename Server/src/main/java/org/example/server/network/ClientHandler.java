package org.example.server.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;

import org.example.core.dto.*;

import org.example.core.dto.admin.AdminBanUserDTO;
import org.example.core.dto.admin.AdminCancelAuctionDTO;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.shared.enums.AuctionStatus;
import org.example.core.shared.enums.ItemStatus;

import org.example.server.daos.AuctionDAO;
import org.example.server.daos.ItemDAO;

import org.example.core.models.items.Item;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.VehicleItem;
import org.example.core.models.users.User;
import org.example.server.daos.UserDAO;
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
import org.example.server.services.BiddingService;
import org.example.server.services.ItemService;
import org.example.server.services.UserService;

public class ClientHandler implements Runnable {
  public static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
  UserDAO userDAO = UserDAO.getInstance();

  private final Socket clientSocket;
  private BufferedReader in;
  private PrintWriter out;

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
            case "UPDATE_ITEM_FULL":
              handleEditProduct(request);
              break;
            case "DELETE_ITEM":
              handleDeleteProduct(request);
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
              handleGetActiveAuctions();
              break;
            case "DEPOSIT":
              handleDeposit(request);
              break;
            case "UPDATE_ROLE":
              handleUpdateRole(request);
              break;
            case "GET_PAID_HISTORY":
              handleGetPaidHistory(request);
              break;
            case "GET_PENDING_PAYMENTS":
              handleGetPendingPayments(request);
              break;
            case "PAY_ITEM":
              handlePayItem(request);
              break;
            case "PAY_ALL":
              handlePayAllItems(request);
              break;
            case "ADMIN_GET_ALL_AUCTIONS":

              break;
            case "ADMIN_PROCESS_ITEM":
              handleAdminProcessItem(request);
              break;
            case "ADMIN_GET_ALL_PENDING_ITEMS":
              handleAdminGetPendingItems(request);
              break;
            case "ADMIN_GET_ALL_USERS":
              handleAdminGetAllUsers(request);
              break;
            case "ADMIN_BAN_USER":
              handleAdminBanUser(request);
              break;
            case "ADMIN_CANCEL_AUCTION":
              handleAdminCancelAuction(request);
              break;
            case "LEAVE_ROOM":
              // Gửi cho con Zombie 1 cục xương để nó nhả hàm readLine() ra
              Response leaveRes = new Response("LEAVE_SUCCESS", "Giải phóng luồng thành công");
              sendMessage(gson.toJson(leaveRes));
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

  private void handleDeleteProduct(Request request) {
    DeleteRequestDTO deleteRequest;
    try {
      if (request.getData() instanceof DeleteRequestDTO) {
        deleteRequest = (DeleteRequestDTO) request.getData();
      } else {
        String dataJson = gson.toJson(request.getData());
        deleteRequest = gson.fromJson(dataJson, DeleteRequestDTO.class);
      }
      boolean success = ItemService.deleteItem(deleteRequest);
      Response response;
      if (success) {
        response = new Response("SUCCESS", "Item deleted successfully.");
      } else {
        response = new Response("ERROR", "Failed to delete item.");
      }
      sendMessage(gson.toJson(response));
    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Server Error: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleEditProduct(Request request) {
    EditProductRequestDTO editRequest;
    try {
      if (request.getData() instanceof EditProductRequestDTO) {
        editRequest = (EditProductRequestDTO) request.getData();
      } else {
        String dataJson = gson.toJson(request.getData());
        editRequest = gson.fromJson(dataJson, EditProductRequestDTO.class);
      }
      boolean success = ItemService.updateItemFull(editRequest);
      Response response;
      if (success) {
        Item item = ItemDAO.getInstance().getItemById(editRequest.getItemId());
        response = new Response("SUCCESS", "Item updated successfully!", item);
      } else {
        response = new Response("ERROR", "Failed to update item.");
      }
      sendMessage(gson.toJson(response));
    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Server Error: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleGetPendingItems(Request request) {
    PendingItemsDTO pendingRequest;

    if (request.getData() instanceof PendingItemsDTO) {
      pendingRequest = (PendingItemsDTO) request.getData();
    } else {
      String dataJson = gson.toJson(request.getData());
      pendingRequest = gson.fromJson(dataJson, PendingItemsDTO.class);
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
      Response response = new Response("SUCCESS", "Đã lên sàn đấu giá thành công!", newAuction);
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
      BidRequestDTO bidReq = gson.fromJson(dataJson, BidRequestDTO.class);

      // Ném xuống BiddingService để xử lý
      boolean success = BiddingService.getInstance().placeBid(bidReq);

      if (success) {

        String realUsername = "Unknown"; // Tên mặc định nếu không tìm thấy

        try {
          User user = userDAO.getUserByUserId(bidReq.getUserId());

          if (user != null) {
            realUsername =
                user.getUserName(); // Lấy tên thật (đảm bảo hàm get tên khớp với class User của
            // bạn)
          }
        } catch (Exception e) {
          System.out.println("Lỗi truy vấn tên người dùng: " + e.getMessage());
        }
        LocalDateTime currentEndTime = null;

        try {
          Auction updatedAuction =
              AuctionDAO.getInstance().getAuctionByAuctionId(bidReq.getAuctionId());
          if (updatedAuction != null) {
            currentEndTime =
                updatedAuction.getEndTime(); // Lấy giờ mới (đã được BiddingService gia hạn nếu có)
          }
        } catch (Exception e) {
          System.out.println("Lỗi lấy thời gian kết thúc: " + e.getMessage());
        }
        // ==========================================

        BidBroadcastDTO broadcastDTO =
            new BidBroadcastDTO(
                bidReq.getAuctionId(),
                bidReq.getBidAmount().doubleValue(),
                realUsername,
                currentEndTime); // Lúc này currentEndTime đã ngậm giờ thật rồi!

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
      List<BidTransaction> history = BiddingService.getInstance().getBidHistory(auctionId);

      // Bọc lại thành Response gửi về cho Client
      Response response = new Response("SUCCESS", "Lấy lịch sử thành công", history);
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi lấy lịch sử: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleDeposit(Request request) {
    UserService userService = new UserService();
    try {
      String dataJson = gson.toJson(request.getData());
      DepositRequestDTO depositRequest = gson.fromJson(dataJson, DepositRequestDTO.class);

      boolean success =
          userService.balanceDeposit(depositRequest.getUserId(), depositRequest.getAmount(),depositRequest.getPassword());

      Response response;
      if (success) {

        // 1. Phải gọi Database để lấy số dư mới nhất của User ra
        java.math.BigDecimal newBalance = userDAO.getUserByUserId(depositRequest.getUserId()).getBalance();

        // 2. NHÉT newBalance VÀO THAM SỐ THỨ 3 CỦA RESPONSE
        response = new Response("SUCCESS", "Nạp tiền thành công!", newBalance);


      } else {
        response = new Response("ERROR", "Nạp tiền thất bại.");
      }

      // CHỈ GỬI 1 LẦN DUY NHẤT Ở ĐÂY
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi nạp tiền: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleGetActiveAuctions() {
    try {
      // Lấy danh sách các sản phẩm từ những phiên đấu giá đang chạy
      List<Auction> activeItems = AuctionService.getAuctionsByStatus(AuctionStatus.RUNNING);

      // Khởi tạo phản hồi thành công
      Response response =
          new Response("SUCCESS", "Lấy danh sách đấu giá đang diễn ra thành công", activeItems);

      // Gửi về cho client yêu cầu
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse =
          new Response("ERROR", "Lỗi khi lấy danh sách đấu giá: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleUpdateRole(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      UpdateRoleRequestDTO update = gson.fromJson(dataJson, UpdateRoleRequestDTO.class);

      boolean success = userDAO.updateRoleInDB(update.getUserId());
      if (success) {
        Response response = new Response("SUCCESS", "Đã nâng cấp lên Seller thành công!!!!");
        sendMessage(gson.toJson(response));
      }
    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Không thể nâng cấp lên Seller???");
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleGetPendingPayments(Request request) {
    try{
      String dataJson = gson.toJson(request.getData());
      int userId = gson.fromJson(dataJson, Integer.class);
      List<PendingPaymentsDTO> pendingPaymentsDTOS = AuctionService.getAllAuctionsFinished(userId);
      Response response = new Response("SUCCESS", "Thanh cong!!!", pendingPaymentsDTOS);
      sendMessage(gson.toJson(response));
    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Khong the gui du lieu");
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handlePayItem(Request request) {
    try{
      String dataJson = gson.toJson(request.getData());
      PendingPaymentsDTO pendingPaymentsDTO = gson.fromJson(dataJson, PendingPaymentsDTO.class);
      System.out.println("Thong tin pendingPaymentsDTO " + pendingPaymentsDTO.getAuctionId() + " " + pendingPaymentsDTO.getUserId());
      int auctionId = pendingPaymentsDTO.getAuctionId();
      int bidderId = pendingPaymentsDTO.getUserId();
      boolean success = AuctionService.checkoutAuction(auctionId, bidderId);
      Response response;
      if (success) {
        response = new Response("SUCCESS", "Thanh toan thanh cong!!!");
        sendMessage(gson.toJson(response));
      } else {
        response = new Response("ERROR", "Khong the thanh toan!!!");
        sendMessage(gson.toJson(response));
      }
    } catch (Exception e) {
      e.printStackTrace();
      Response response = new Response("ERROR", "Loi giao dich");
      sendMessage(gson.toJson(response));
    }
  }

  private void handlePayAllItems(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      int userId = gson.fromJson(dataJson, Integer.class);
      List<Integer> auctionIds = AuctionDAO.getInstance().getAllAuctionIdFinishedByUserId(userId);
      for (Integer x: auctionIds) {
        AuctionService.checkoutAuction(x, userId);
      }
      Response response = new Response("SUCCESS", "Thanh toan toan bo thanh cong!!!");
      sendMessage(gson.toJson(response));
    } catch (Exception e) {
      e.printStackTrace();
      Response response = new Response("ERROR", "Thanh toan khong thanh cong");
      sendMessage(gson.toJson(response));
    }
  }

  private void handleGetPaidHistory(Request request) {
    try {
      // Giả sử Client gửi data chính là cái userId (kiểu int)
      String dataJson = gson.toJson(request.getData());
      Integer userId = gson.fromJson(dataJson, Integer.class);

      // Gọi Service lấy danh sách lịch sử
      List<PaidHistoryDTO> paidHistoryDTO = AuctionService.getAllAuctionsPaid(userId);

      // Bọc lại thành Response gửi về cho Client
      Response response = new Response("SUCCESS", "Lấy lịch sử thanh toán thành công", paidHistoryDTO);
      sendMessage(gson.toJson(response));

        } catch (Exception e) {
          e.printStackTrace();
          Response errorResponse = new Response("ERROR", "Lỗi lấy lịch sử thanh toán: " + e.getMessage());
          sendMessage(gson.toJson(errorResponse));
        }
      }

  public synchronized void sendMessage(String message) {
    out.println(message);
  }

  // SỬA LẠI HÀM NÀY: Phải gạch tên khách khỏi sổ khi nó out!
  private void closeConnection() {
    try {
      // BƯỚC QUAN TRỌNG NHẤT: Đuổi nó khỏi danh sách Broadcast!
      connectedClients.remove(this);
      System.out.println(
          "[SERVER] Một Client vừa thoát. Tổng số người online: " + connectedClients.size());

      if (in != null) in.close();
      if (out != null) out.close();
      if (clientSocket != null) clientSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // SỬA LẠI HÀM NÀY: Bọc thêm try-catch để lỡ 1 thằng lỗi thì không chết lây thằng khác
  public static void broadcastMessage(String message) {
    for (ClientHandler client : connectedClients) {
      try {
        client.sendMessage(message);
      } catch (Exception e) {
        System.err.println("Lỗi khi gửi Broadcast cho 1 client: " + e.getMessage());
      }
    }
  }

  // 1. XỬ LÝ DUYỆT / TỪ CHỐI TÀI SẢN
  private void handleAdminProcessItem(Request request) {

    try {
      String dataJson = gson.toJson(request.getData());
      org.example.core.dto.admin.AdminProcessItemDTO processReq = gson.fromJson(dataJson, org.example.core.dto.admin.AdminProcessItemDTO.class);

      org.example.core.models.users.User requester = org.example.server.daos.UserDAO.getInstance().getUserByUserId(processReq.getAdminId());
      if (requester == null || requester.getRole() != org.example.core.shared.enums.RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return; // Đuổi cổ ngay
      }

      org.example.core.models.items.Item checkItem = org.example.server.daos.ItemDAO.getInstance().getItemById(processReq.getItemId());
      if (checkItem.getStatus() != org.example.core.shared.enums.ItemStatus.PENDING) {
        Response errorResponse = new Response("ERROR", "Lỗi: Tài sản này không ở trạng thái Chờ Duyệt!");
        sendMessage(gson.toJson(errorResponse));
        return; // Dừng luôn, không cho duyệt nữa
      }
      ItemStatus newStatus = processReq.isApproved() ? ItemStatus.APPROVED : ItemStatus.REJECTED;
      boolean success = ItemDAO.getInstance().updateItemStatus(processReq.getItemId(), newStatus);
      if (success) {
        String msg = processReq.isApproved() ? "Đã DUYỆT tài sản thành công!" : "Đã TỪ CHỐI tài sản!";
        Response response = new Response("SUCCESS", msg);
        sendMessage(gson.toJson(response));
      } else {
        Response errorResponse = new Response("ERROR", "Lỗi: Không thể cập nhật trạng thái tài sản trong DB.");
        sendMessage(gson.toJson(errorResponse));
      }
    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi Server: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  // 2. LẤY DANH SÁCH USER
  private void handleAdminGetAllUsers(Request request) {
    try {
      // 1. Lấy adminId từ request (Giả sử Client gửi thẳng số Integer)
      String dataJson = gson.toJson(request.getData());
      Integer adminId = gson.fromJson(dataJson, Integer.class);

      // 2. CHECK QUYỀN TRƯỚC KHI LÀM:
      org.example.core.models.users.User requester = org.example.server.daos.UserDAO.getInstance().getUserByUserId(adminId);
      if (requester == null || requester.getRole() != org.example.core.shared.enums.RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return;
      }

      // 3. Đúng Admin rồi mới cho kéo list
      List<User> users = org.example.server.daos.UserDAO.getInstance().getAllUsers();
      Response response = new Response("SUCCESS", "Lấy danh sách User thành công", users);
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi khi lấy danh sách User: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }
  // 3. KHÓA / MỞ KHÓA TÀI KHOẢN
  private void handleAdminBanUser(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      AdminBanUserDTO banReq = gson.fromJson(dataJson, AdminBanUserDTO.class);

      org.example.core.models.users.User requester = org.example.server.daos.UserDAO.getInstance().getUserByUserId(banReq.getAdminId());
      if (requester == null || requester.getRole() != org.example.core.shared.enums.RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return; // Đuổi cổ ngay
      }

      boolean success;

      // Kiểm tra xem Admin muốn Khóa hay Mở khóa để gọi đúng hàm trong UserDAO
      if (banReq.isBanned()) {
        success = org.example.server.daos.UserDAO.getInstance().banStatus(banReq.getUserId());
      } else {
        success = org.example.server.daos.UserDAO.getInstance().unbanStatus(banReq.getUserId());
      }

      if (success) {
        String msg = banReq.isBanned() ? "Đã KHÓA tài khoản user thành công!" : "Đã MỞ KHÓA tài khoản user!";
        Response response = new Response("SUCCESS", msg);
        sendMessage(gson.toJson(response));
      } else {
        Response errorResponse = new Response("ERROR", "Lỗi DB: Không thể cập nhật trạng thái tài khoản.");
        sendMessage(gson.toJson(errorResponse));
      }
    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi Server: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  // 3. HỦY KHẨN CẤP PHIÊN ĐẤU GIÁ (ADMIN)
  private void handleAdminCancelAuction(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      AdminCancelAuctionDTO cancelReq = gson.fromJson(dataJson, AdminCancelAuctionDTO.class);

      // CHECK QUYỀN TRƯỚC KHI LÀM:
      org.example.core.models.users.User requester = org.example.server.daos.UserDAO.getInstance().getUserByUserId(cancelReq.getAdminId());
      if (requester == null || requester.getRole() != org.example.core.shared.enums.RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return; // Đuổi cổ ngay
      }

      org.example.server.services.AuctionService.forceCancelAuction(cancelReq.getAuctionId(), "Admin hủy khẩn cấp");

        Response response = new Response("SUCCESS", "Đã HỦY KHẨN CẤP phiên đấu giá!");
        sendMessage(gson.toJson(response));

        Response broadcast = new Response("AUCTION_END", "Phiên đấu giá bị Admin hủy bỏ khẩn cấp!", "ADMIN_CANCELLED");
        broadcastMessage(gson.toJson(broadcast));

    } catch (Exception e) {
      // Nếu ID không tồn tại hoặc phiên đã bị hủy từ trước, Service sẽ ném lỗi ra đây
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi Server: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  // LẤY DANH SÁCH TÀI SẢN CHỜ DUYỆT (ADMIN)
  private void handleAdminGetPendingItems(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      Integer adminId = gson.fromJson(dataJson, Integer.class);

      org.example.core.models.users.User requester = org.example.server.daos.UserDAO.getInstance().getUserByUserId(adminId);
      if (requester == null || requester.getRole() != org.example.core.shared.enums.RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return;
      }

      List<Item> pendingItems = org.example.server.daos.ItemDAO.getInstance().getItemsByStatus(ItemStatus.PENDING);

      Response response = new Response("SUCCESS", "Lấy danh sách chờ duyệt thành công", pendingItems);
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi Server khi lấy danh sách chờ duyệt: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }
}
