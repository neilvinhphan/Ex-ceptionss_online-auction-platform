package org.example.server.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.admin.AdminBanUserDTO;
import org.example.core.dto.admin.AdminDashboardDTO;
import org.example.core.dto.admin.AdminProcessItemDTO;
import org.example.core.dto.auctionDTO.CreateAuctionDTO;
import org.example.core.dto.bidDTO.AutoBidRequestDTO;
import org.example.core.dto.bidDTO.BidBroadcastDTO;
import org.example.core.dto.bidDTO.BidRequestDTO;
import org.example.core.dto.itemsDTO.CreateArtItemDTO;
import org.example.core.dto.itemsDTO.CreateElectronicsItemDTO;
import org.example.core.dto.itemsDTO.CreateItemRequestDTO;
import org.example.core.dto.itemsDTO.CreateVehicleItemDTO;
import org.example.core.dto.itemsDTO.DeleteRequestDTO;
import org.example.core.dto.itemsDTO.EditProductRequestDTO;
import org.example.core.dto.itemsDTO.PendingItemsDTO;
import org.example.core.dto.paymentDTO.PaidHistoryDTO;
import org.example.core.dto.paymentDTO.PendingPaymentsDTO;
import org.example.core.dto.userDTO.DepositRequestDTO;
import org.example.core.dto.userDTO.LoginRequestDTO;
import org.example.core.dto.userDTO.RegisterRequestDTO;
import org.example.core.dto.userDTO.SellerDashboardDTO;
import org.example.core.dto.userDTO.UpdateRoleRequestDTO;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;
import org.example.core.models.users.User;
import org.example.core.network.LocalDateTimeAdapter;
import org.example.core.shared.enums.AuctionStatus;
import org.example.core.shared.enums.ItemStatus;
import org.example.core.shared.enums.RoleType;
import org.example.server.services.AuctionService;
import org.example.server.services.AuthService;
import org.example.server.services.BiddingService;
import org.example.server.services.DashBoardService;
import org.example.server.services.ItemService;
import org.example.server.services.UserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
  private static final ConcurrentHashMap<Integer, ClientHandler> activeUsers =
      new ConcurrentHashMap<>();

  private static final AuthService authService = AuthService.getInstance();
  private static final UserService userService = UserService.getInstance();
  private static final ItemService itemService = ItemService.getInstance();
  private static final AuctionService auctionService = AuctionService.getInstance();
  private static final BiddingService biddingService = BiddingService.getInstance();
  private static final DashBoardService dashBoardService = DashBoardService.getInstance();

  private final Socket clientSocket;
  private BufferedReader in;
  private PrintWriter out;
  private int currentRoomId = -1;
  private int userId = -1;

  private final Gson gson =
      new GsonBuilder()
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

  /** Khởi tạo bộ gom kênh I/O truyền thông cho kết nối Socket Client mới. */
  public ClientHandler(Socket clientSocket) {
    this.clientSocket = clientSocket;
    try {
      this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      this.out = new PrintWriter(clientSocket.getOutputStream(), true);
      connectedClients.add(this);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Lỗi thiết lập kênh dữ liệu I/O Handler Client", e);
      throw new RuntimeException("Khởi tạo Handler liên kết kết nối thất bại", e);
    }
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

        switch (request.getAction()) {
          case "REGISTER" -> handleRegister(request);
          case "LOGIN" -> handleLogin(request);
          case "LOGOUT" -> handleLogout(request);
          case "CREATE_ITEM" -> handleCreateItem(request);
          case "UPDATE_ITEM_FULL" -> handleEditProduct(request);
          case "DELETE_ITEM" -> handleDeleteProduct(request);
          case "GET_PENDING_ITEMS" -> handleGetPendingItems(request);
          case "GET_APPROVED_ITEMS" -> handleGetApprovedItems(request);
          case "CREATE_AUCTION" -> handleCreateAuction(request);
          case "PLACE_BID" -> handlePlaceBid(request);
          case "GET_BID_HISTORY" -> handleGetBidHistory(request);
          case "GET_ACTIVE_AUCTIONS" -> handleGetActiveAuctions();
          case "DEPOSIT" -> handleDeposit(request);
          case "UPDATE_ROLE" -> handleUpdateRole(request);
          case "GET_PAID_HISTORY" -> handleGetPaidHistory(request);
          case "GET_PENDING_PAYMENTS" -> handleGetPendingPayments(request);
          case "PAY_ITEM" -> handlePayItem(request);
          case "PAY_ALL" -> handlePayAllItems(request);
          case "JOIN_ROOM" -> handleJoinRoom(request);
          case "LEAVE_ROOM" -> handleLeaveRoom(request);
          case "REGISTER_AUTOBID" -> handleRegisterAutoBid(request);
          case "CANCEL_AUTOBID" -> handleCancelAutoBid(request);
          case "GET_MARKET_HISTORY" -> handleGetMarketHistory(request);
          case "GET_ADMIN_DASHBOARD_STATS" -> handleGetAdminDashboardStats(request);
          case "GET_SELLER_DASHBOARD" -> handleGetSellerDashboard(request);
          case "ADMIN_GET_ALL_AUCTIONS" -> handleAdminGetAllAuctions();
          case "ADMIN_PROCESS_ITEM" -> handleAdminProcessItem(request);
          case "ADMIN_GET_ALL_PENDING_ITEMS" -> handleAdminGetPendingItems(request);
          case "ADMIN_GET_ALL_USERS" -> handleAdminGetAllUsers(request);
          case "ADMIN_BAN_USER" -> handleAdminBanUser(request);
          case "ADMIN_CANCEL_AUCTION" -> handleAdminCancelAuction(request);
          case "GET_PROMOTED_AUCTIONS" -> {
            sendMessage(gson.toJson(new Response("SUCCESS", "Chưa có dữ liệu", null)));
          }
          default ->
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

  // --- NHÓM PHƯƠNG THỨC XỬ LÝ ĐỊNH TUYẾN NGHIỆP VỤ (BUSINESS ROUTER) ---

  private void handleRegister(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      RegisterRequestDTO registerRequest = gson.fromJson(dataJson, RegisterRequestDTO.class);
      User newUser = authService.register(registerRequest);

      Response response =
          (newUser != null)
              ? new Response("SUCCESS", "Registration successful")
              : new Response("ERROR", "Registration failed");
      sendMessage(gson.toJson(response));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi đăng ký tài khoản", e);
      sendMessage(gson.toJson(new Response("ERROR", e.getMessage())));
    }
  }

  private void handleLogin(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      LoginRequestDTO loginRequest = gson.fromJson(dataJson, LoginRequestDTO.class);
      User newUser = authService.login(loginRequest);

      if (newUser != null) {
        if (activeUsers.containsKey(newUser.getUserId())) {
          sendMessage(
              gson.toJson(
                  new Response(
                      "ERROR", "Tài khoản này đang được đăng nhập ở một thiết bị khác!", null)));
          return;
        }
        this.userId = newUser.getUserId();
        activeUsers.put(this.userId, this);
        sendMessage(gson.toJson(new Response("SUCCESS", "Login success!", newUser)));
      } else {
        sendMessage(gson.toJson(new Response("ERROR", "Login failed")));
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi xử lý đăng nhập", e);
      sendMessage(gson.toJson(new Response("ERROR", e.getMessage())));
    }
  }

  private void handleLogout(Request request) {
    try {
      if (this.userId != -1) {
        activeUsers.remove(this.userId);
        System.out.println("[SERVER] User ID " + this.userId + " đã Đăng xuất an toàn.");
        this.userId = -1;
      }
      sendMessage(gson.toJson(new Response("SUCCESS", "Đăng xuất thành công!", null)));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleCreateItem(Request request) {
    try {
      String rawDataJson = gson.toJson(request.getData());
      JsonObject jsonObject = gson.fromJson(rawDataJson, JsonObject.class);
      String type = jsonObject.get("type").getAsString();

      CreateItemRequestDTO finalDTO =
          switch (type.toUpperCase()) {
            case "ART" -> gson.fromJson(rawDataJson, CreateArtItemDTO.class);
            case "VEHICLE" -> gson.fromJson(rawDataJson, CreateVehicleItemDTO.class);
            case "ELECTRONICS" -> gson.fromJson(rawDataJson, CreateElectronicsItemDTO.class);
            default -> gson.fromJson(rawDataJson, CreateItemRequestDTO.class);
          };

      Item newItem = itemService.createItem(finalDTO);
      Response response =
          (newItem != null)
              ? new Response("SUCCESS", "Item created successfully!", newItem)
              : new Response("ERROR", "Failed to create item.");
      sendMessage(gson.toJson(response));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi đăng ký sản phẩm mới", e);
      sendMessage(gson.toJson(new Response("ERROR", "Server Error: " + e.getMessage())));
    }
  }

  private void handleEditProduct(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      EditProductRequestDTO editRequest = gson.fromJson(dataJson, EditProductRequestDTO.class);

      if (itemService.updateItemFull(editRequest)) {
        Item item = itemService.getItemById(editRequest.getItemId());
        sendMessage(gson.toJson(new Response("SUCCESS", "Item updated successfully!", item)));
      } else {
        sendMessage(gson.toJson(new Response("ERROR", "Failed to update item.")));
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi cập nhật chỉnh sửa sản phẩm", e);
      sendMessage(gson.toJson(new Response("ERROR", "Server Error: " + e.getMessage())));
    }
  }

  private void handleDeleteProduct(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      DeleteRequestDTO deleteRequest = gson.fromJson(dataJson, DeleteRequestDTO.class);

      Response response =
          itemService.deleteItem(deleteRequest)
              ? new Response("SUCCESS", "Item deleted successfully.")
              : new Response("ERROR", "Failed to delete item.");
      sendMessage(gson.toJson(response));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi xóa bỏ sản phẩm", e);
      sendMessage(gson.toJson(new Response("ERROR", "Server Error: " + e.getMessage())));
    }
  }

  private void handleGetPendingItems(Request request) {
    try {
      PendingItemsDTO pendingRequest =
          (request.getData() instanceof PendingItemsDTO dto)
              ? dto
              : gson.fromJson(gson.toJson(request.getData()), PendingItemsDTO.class);

      List<Item> items = itemService.getAllItem(pendingRequest);
      sendMessage(
          gson.toJson(new Response("SUCCESS", "Fetched pending items successfully!", items)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi trích xuất kho đồ chờ duyệt của user", e);
      sendMessage(
          gson.toJson(new Response("ERROR", "Failed to fetch pending items: " + e.getMessage())));
    }
  }

  private void handleGetApprovedItems(Request request) {
    try {
      PendingItemsDTO dto = gson.fromJson(gson.toJson(request.getData()), PendingItemsDTO.class);
      List<Item> approvedItems = itemService.getApprovedItemsByUserId(dto.getSellerId());
      sendMessage(
          gson.toJson(new Response("SUCCESS", "Tải danh sách sản phẩm thành công", approvedItems)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi trích xuất kho hàng khả dụng ra sàn", e);
      sendMessage(
          gson.toJson(new Response("ERROR", "Không thể tải danh sách tài sản: " + e.getMessage())));
    }
  }

  private void handleCreateAuction(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      CreateAuctionDTO auctionReq = gson.fromJson(dataJson, CreateAuctionDTO.class);

      Auction newAuction = auctionService.createAuction(auctionReq);
      itemService.updateItemStatus(auctionReq.getItem().getItemId(), ItemStatus.LISTED);

      sendMessage(
          gson.toJson(new Response("SUCCESS", "Đã lên sàn đấu giá thành công!", newAuction)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi đẩy sản phẩm lên sàn đấu giá", e);
      sendMessage(gson.toJson(new Response("ERROR", "Lỗi tạo đấu giá: " + e.getMessage())));
    }
  }

  private void handlePlaceBid(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      BidRequestDTO bidReq = gson.fromJson(dataJson, BidRequestDTO.class);

      if (biddingService.placeBid(bidReq)) {
        LocalDateTime currentEndTime = null;
        try {
          Auction updatedAuction = auctionService.getAuctionById(bidReq.getAuctionId());
          if (updatedAuction != null) currentEndTime = updatedAuction.getEndTime();
        } catch (Exception e) {
          logger.log(Level.WARNING, "Không lấy được thời gian kết thúc cập nhật để đồng bộ");
        }

        BidBroadcastDTO broadcastDTO =
            new BidBroadcastDTO(
                bidReq.getAuctionId(),
                bidReq.getBidAmount().doubleValue(),
                bidReq.getUserName(),
                currentEndTime);

        broadcastMessage(
            gson.toJson(new Response("NEW_BID", "Có người vừa đặt giá mới", broadcastDTO)));
        biddingService.evaluateDeterministicBidding(bidReq.getAuctionId());
      } else {
        sendMessage(gson.toJson(new Response("ERROR_BID", "Đặt giá không thành công.")));
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi xử lý đặt thầu trực tiếp từ Client", e);
      sendMessage(gson.toJson(new Response("ERROR_BID", e.getMessage())));
    }
  }

  private void handleGetBidHistory(Request request) {
    try {
      Integer auctionId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
      List<BidTransaction> history = biddingService.getBidHistory(auctionId);
      sendMessage(gson.toJson(new Response("SUCCESS", "Lấy lịch sử thành công", history)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi truy vấn lịch sử biểu đồ thầu", e);
      sendMessage(gson.toJson(new Response("ERROR", "Lỗi lấy lịch sử: " + e.getMessage())));
    }
  }

  private void handleGetActiveAuctions() {
    try {
      List<Auction> activeItems = new ArrayList<>();
      List<Auction> runningAuctions = auctionService.getAuctionsByStatus(AuctionStatus.RUNNING);
      List<Auction> openAuctions = auctionService.getAuctionsByStatus(AuctionStatus.OPEN);

      if (runningAuctions != null) activeItems.addAll(runningAuctions);
      if (openAuctions != null) activeItems.addAll(openAuctions);

      sendMessage(
          gson.toJson(
              new Response(
                  "SUCCESS", "Lấy danh sách đấu giá đang diễn ra thành công", activeItems)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi tải dữ liệu các phiên active ngoài sảnh chính", e);
      sendMessage(
          gson.toJson(new Response("ERROR", "Lỗi khi lấy danh sách đấu giá: " + e.getMessage())));
    }
  }

  private void handleDeposit(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      DepositRequestDTO depositRequest = gson.fromJson(dataJson, DepositRequestDTO.class);

      BigDecimal newBalance =
          userService.balanceDeposit(
              depositRequest.getUserId(), depositRequest.getAmount(), depositRequest.getPassword());

      Response response =
          (newBalance != null)
              ? new Response("SUCCESS", "Nạp tiền thành công!", newBalance)
              : new Response("ERROR", "Nạp tiền thất bại.");
      sendMessage(gson.toJson(response));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi xử lý nạp ví điện tử", e);
      sendMessage(gson.toJson(new Response("ERROR", "Lỗi nạp tiền: " + e.getMessage())));
    }
  }

  private void handleUpdateRole(Request request) {
    try {
      UpdateRoleRequestDTO requestDTO =
          gson.fromJson(gson.toJson(request.getData()), UpdateRoleRequestDTO.class);
      if (userService.updateRole(requestDTO.getUserId())) {
        sendMessage(gson.toJson(new Response("SUCCESS", "Đã nâng cấp lên Seller thành công!!!!")));
      } else {
        sendMessage(gson.toJson(new Response("ERROR", "Nâng cấp thất bại! Lỗi cơ sở dữ liệu.")));
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi nâng cấp định danh vai trò User", e);
      sendMessage(gson.toJson(new Response("ERROR", "Không thể nâng cấp lên Seller???")));
    }
  }

  private void handleGetPendingPayments(Request request) {
    try {
      int targetUserId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
      List<PendingPaymentsDTO> pendingPaymentsDTOS =
          auctionService.getAllAuctionsFinished(targetUserId);
      sendMessage(gson.toJson(new Response("SUCCESS", "Thanh cong!!!", pendingPaymentsDTOS)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi nạp danh sách hóa đơn pending", e);
      sendMessage(gson.toJson(new Response("ERROR", "Khong the gui du lieu")));
    }
  }

  private void handlePayItem(Request request) {
    try {
      PendingPaymentsDTO pendingPaymentsDTO =
          gson.fromJson(gson.toJson(request.getData()), PendingPaymentsDTO.class);
      Response response =
          auctionService.checkoutAuction(
                  pendingPaymentsDTO.getAuctionId(), pendingPaymentsDTO.getUserId())
              ? new Response("SUCCESS", "Thanh toan thanh cong!!!")
              : new Response("ERROR", "Khong the thanh toan!!!");
      sendMessage(gson.toJson(response));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi giao dịch thanh toán đơn lẻ hóa đơn", e);
      sendMessage(gson.toJson(new Response("ERROR", "Loi giao dich")));
    }
  }

  private void handlePayAllItems(Request request) {
    try {
      int targetUserId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
      List<Integer> auctionIds = auctionService.getAllItemPaidPending(targetUserId);
      for (Integer x : auctionIds) {
        auctionService.checkoutAuction(x, targetUserId);
      }
      sendMessage(gson.toJson(new Response("SUCCESS", "Thanh toan toan bo thanh cong!!!")));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi xử lý thanh toán thanh lý hàng loạt hóa đơn", e);
      sendMessage(gson.toJson(new Response("ERROR", "Thanh toan khong thanh cong")));
    }
  }

  private void handleGetPaidHistory(Request request) {
    try {
      Integer targetUserId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
      List<PaidHistoryDTO> paidHistoryDTO = auctionService.getAllAuctionsPaid(targetUserId);
      sendMessage(
          gson.toJson(
              new Response("SUCCESS", "Lấy lịch sử thanh toán thành công", paidHistoryDTO)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi lấy nhật ký giao dịch chi tiền ví", e);
      sendMessage(
          gson.toJson(new Response("ERROR", "Lỗi lấy lịch sử thanh toán: " + e.getMessage())));
    }
  }

  private void handleJoinRoom(Request request) {
    try {
      JsonObject jsonObject = gson.fromJson(gson.toJson(request.getData()), JsonObject.class);
      int auctionId = jsonObject.get("auctionId").getAsInt();

      this.currentRoomId = auctionId;
      AuctionServer.addClientToRoom(auctionId, this);
      sendMessage(gson.toJson(new Response("SUCCESS", "Đã tham gia phòng " + auctionId)));

      if (this.userId != -1) {
        BigDecimal savedMaxBid = biddingService.getMaxAutoBid(auctionId, this.userId);
        if (savedMaxBid != null) {
          Map<String, Object> autoBidState = new HashMap<>();
          autoBidState.put("maxBid", savedMaxBid.doubleValue());
          sendMessage(
              gson.toJson(
                  new Response("MY_AUTOBID_STATUS", "Khôi phục trạng thái Bot", autoBidState)));
          logger.info(
              "🤖 [RE-JOIN] Đã gửi gói khôi phục trạng thái Bot cho User "
                  + this.userId
                  + " tại phòng "
                  + auctionId);
        }
        AuctionServer.broadcastRoomUserCount(auctionId);
      }

      // Đồng bộ dữ liệu phòng đấu giá mới nhất từ DB khi tham gia phòng (phòng hờ vào phòng
      // FINISHED)
      Auction freshAuction = AuctionService.getInstance().getAuctionByAuctionId(auctionId);
      if (freshAuction != null) {
        if (freshAuction.getBidderId() > 0) {
          User winner = UserService.getInstance().getUserById(freshAuction.getBidderId());
          if (winner != null) freshAuction.setHighestBidderName(winner.getUserName());
        }
        sendMessage(
            gson.toJson(
                new Response(
                    "INITIAL_ROOM_DATA", "Dữ liệu khởi tạo phòng mới nhất", freshAuction)));
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi xử lý gia nhập phòng", e);
      sendMessage(gson.toJson(new Response("ERROR", "Lỗi khi join phòng: " + e.getMessage())));
    }
  }

  private void handleLeaveRoom(Request request) {
    int oldRoomId = this.currentRoomId;
    if (this.currentRoomId != -1) {
      AuctionServer.removeClientFromRoom(this.currentRoomId, this);
      this.currentRoomId = -1;
    }
    AuctionServer.broadcastRoomUserCount(oldRoomId);
    sendMessage(gson.toJson(new Response("LEAVE_SUCCESS", "Giải phóng luồng thành công")));
  }

  private void handleRegisterAutoBid(Request request) {
    try {
      AutoBidRequestDTO regDto =
          gson.fromJson(gson.toJson(request.getData()), AutoBidRequestDTO.class);
      biddingService.saveOrUpdateAutoBid(
          regDto.getAuctionId(), regDto.getUserId(), regDto.getMaxBid());

      sendMessage(
          gson.toJson(
              new Response("SUCCESS", "Kích hoạt hệ thống AutoBid gác phòng thành công!", null)));
      biddingService.evaluateDeterministicBidding(regDto.getAuctionId());
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi kích hoạt Bot AutoBid", e);
      sendMessage(
          gson.toJson(new Response("ERROR", "Lỗi kích hoạt AutoBid: " + e.getMessage(), null)));
    }
  }

  private void handleCancelAutoBid(Request request) {
    try {
      AutoBidRequestDTO cancelDto =
          gson.fromJson(gson.toJson(request.getData()), AutoBidRequestDTO.class);
      biddingService.disableAutoBid(cancelDto.getAuctionId(), cancelDto.getUserId());
      sendMessage(
          gson.toJson(
              new Response("SUCCESS", "Đã hủy hệ thống tự động trả giá thành công!", null)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi hủy chế độ đặt giá tự động", e);
      sendMessage(gson.toJson(new Response("ERROR", "Lỗi hủy AutoBid: " + e.getMessage(), null)));
    }
  }

  private void handleGetMarketHistory(Request request) {
    try {
      List<Auction> history = auctionService.getMarketHistory();
      sendMessage(gson.toJson(new Response("SUCCESS", "Dữ liệu lịch sử thị trường", history)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi trích xuất Market History cho catalog kết quả", e);
      sendMessage(
          gson.toJson(
              new Response("ERROR", "Lỗi lấy lịch sử thị trường: " + e.getMessage(), null)));
    }
  }

  private void handleGetSellerDashboard(Request request) {
    try {
      Integer sellerId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
      SellerDashboardDTO dto = dashBoardService.getSellerDashboard(sellerId);
      sendMessage(gson.toJson(new Response("SUCCESS", "Lấy dữ liệu thành công", dto)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi kết xuất thống kê doanh số Seller", e);
      sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage())));
    }
  }

  private void handleGetAdminDashboardStats(Request request) {
    try {
      AdminDashboardDTO dashboardDTO =
          new AdminDashboardDTO(
              dashBoardService.getKPIs(),
              dashBoardService.getCategoryDistribution(),
              dashBoardService.getAuctionStatusDistribution());
      sendMessage(
          gson.toJson(new Response("SUCCESS", "Lấy dữ liệu Dashboard thành công", dashboardDTO)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi kết xuất KPI hệ thống của Admin", e);
      sendMessage(
          gson.toJson(
              new Response("ERROR", "Lỗi Server khi lấy số liệu Dashboard: " + e.getMessage())));
    }
  }

  // --- NHÓM PHƯƠNG THỨC XỬ LÝ QUẢN TRỊ VIÊN (ADMIN ACTIONS ROUTER) ---

  private void handleAdminProcessItem(Request request) {
    try {
      AdminProcessItemDTO processReq =
          gson.fromJson(gson.toJson(request.getData()), AdminProcessItemDTO.class);
      Item checkItem = itemService.getItemById(processReq.getItemId());

      if (checkItem == null) {
        sendMessage(
            gson.toJson(
                new Response(
                    "ERROR", "Lỗi: Không tìm thấy tài sản ID = " + processReq.getItemId())));
        return;
      }
      if (checkItem.getStatus() != ItemStatus.PENDING) {
        sendMessage(
            gson.toJson(new Response("ERROR", "Lỗi: Tài sản này không ở trạng thái PENDING)!")));
        return;
      }

      ItemStatus newStatus = processReq.isApproved() ? ItemStatus.APPROVED : ItemStatus.REJECTED;
      if (itemService.updateItemStatus(processReq.getItemId(), newStatus)) {
        String msg =
            processReq.isApproved() ? "Đã DUYỆT tài sản thành công!" : "Đã TỪ CHỐI tài sản!";
        sendMessage(gson.toJson(new Response("SUCCESS", msg)));
      } else {
        sendMessage(gson.toJson(new Response("ERROR", "Lỗi DB: Không thể cập nhật trạng thái.")));
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi kiểm duyệt phê duyệt tài sản", e);
      sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage())));
    }
  }

  private void handleAdminGetAllUsers(Request request) {
    try {
      Integer adminId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
      User requester = userService.getUserById(adminId);

      if (requester == null || requester.getRole() != RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return;
      }

      List<User> users = UserService.getInstance().getAllUsers();
      sendMessage(gson.toJson(new Response("SUCCESS", "Lấy danh sách User thành công", users)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi phân hệ quản lý người dùng", e);
      sendMessage(
          gson.toJson(new Response("ERROR", "Lỗi khi lấy danh sách User: " + e.getMessage())));
    }
  }

  private void handleAdminGetAllAuctions() {
    try {
      List<Auction> allAuctions = new ArrayList<>();
      for (AuctionStatus status : AuctionStatus.values()) {
        List<Auction> listByStatus = auctionService.getAuctionsByStatus(status);
        if (listByStatus != null) allAuctions.addAll(listByStatus);
      }
      sendMessage(
          gson.toJson(
              new Response("SUCCESS", "Lấy toàn bộ danh sách Auctions thành công", allAuctions)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi tải toàn bộ kho dữ liệu phòng đấu giá cho Admin", e);
      sendMessage(
          gson.toJson(
              new Response(
                  "ERROR", "Lỗi không thể lấy được danh sách auctions: " + e.getMessage())));
    }
  }

  private void handleAdminBanUser(Request request) {
    try {
      AdminBanUserDTO banReq = gson.fromJson(gson.toJson(request.getData()), AdminBanUserDTO.class);
      User requester = userService.getUserById(banReq.getAdminId());

      if (requester == null || requester.getRole() != RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return;
      }

      boolean success =
          banReq.isBanned()
              ? userService.banUser(banReq.getUserId())
              : userService.unbanUser(banReq.getUserId());
      if (success) {
        String msg =
            banReq.isBanned() ? "Đã KHÓA tài khoản user thành công!" : "Đã MỞ KHÓA tài khoản user!";
        sendMessage(gson.toJson(new Response("SUCCESS", msg)));
      } else {
        sendMessage(
            gson.toJson(new Response("ERROR", "Lỗi DB: Không thể cập nhật trạng thái tài khoản.")));
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi thực thi lệnh ban/unban người dùng", e);
      sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage())));
    }
  }

  private void handleAdminCancelAuction(Request request) {
    try {
      Integer auctionId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
      auctionService.forceCancelAuction(auctionId, "Admin hủy khẩn cấp");

      sendMessage(gson.toJson(new Response("SUCCESS", "Đã HỦY KHẨN CẤP phiên đấu giá!")));
      broadcastMessage(
          gson.toJson(
              new Response(
                  "AUCTION_END", "Phiên đấu giá bị Admin hủy bỏ khẩn cấp!", "ADMIN_CANCELLED")));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi thực thi hủy phòng khẩn cấp từ Admin", e);
      sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage())));
    }
  }

  private void handleAdminGetPendingItems(Request request) {
    try {
      Integer adminId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
      User requester = userService.getUserById(adminId);

      if (requester == null || requester.getRole() != RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return;
      }

      List<Item> pendingItems = itemService.getAllItemByStatus(ItemStatus.PENDING);
      sendMessage(
          gson.toJson(new Response("SUCCESS", "Lấy danh sách chờ duyệt thành công", pendingItems)));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi trích xuất kho hàng xếp hàng chờ duyệt", e);
      sendMessage(
          gson.toJson(
              new Response("ERROR", "Lỗi Server khi lấy danh sách chờ duyệt: " + e.getMessage())));
    }
  }

  // --- NHÓM PHƯƠNG THỨC GIẢI PHÓNG HẠ TẦNG KẾT NỐI (CLEANUP SYSTEM) ---

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
}
