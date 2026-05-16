package org.example.server.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;

import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.admin.AdminBanUserDTO;
import org.example.core.dto.admin.AdminCancelAuctionDTO;
import org.example.core.dto.admin.AdminProcessItemDTO;
import org.example.core.dto.auctionDTO.CreateAuctionDTO;
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
import org.example.core.dto.userDTO.UpdateRoleRequestDTO;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.shared.enums.AuctionStatus;
import org.example.core.shared.enums.ItemStatus;
import org.example.core.shared.enums.RoleType;

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
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.example.core.network.LocalDateTimeAdapter;
import org.example.server.services.BiddingService;
import org.example.server.services.ItemService;
import org.example.server.services.UserService;

public class ClientHandler implements Runnable {
  public static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
  UserDAO userDAO = UserDAO.getInstance();
  ItemDAO itemDAO = ItemDAO.getInstance();

  private final Socket clientSocket;
  private BufferedReader in;
  private PrintWriter out;

  private int currentRoomId = -1;

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
              handleAdminGetAllAuctions();
              break;
            case "ADMIN_PROCESS_ITEM":
              handleAdminProcessItem(request);
              break;
            case "ADMIN_GET_ALL_DAFT_ITEMS":
              handleAdminGetDaftItems(request);
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
            case "GET_PENDING_AUCTIONS":
              handleGetPendingAuctions(request);
              break;
            case "JOIN_ROOM":
              handleJoinRoom(request);
              break;
            case "APPROVE_AUCTION":
             // handleApproveAuction(request);
              break;
            case "GET_PROMOTED_AUCTIONS":
              // TRẢ VỀ DỮ LIỆU RỖNG ĐỂ CLIENT KHÔNG BỊ TREO
              Response dummyResponse = new Response("SUCCESS", "Chưa có dữ liệu", null);
              sendMessage(gson.toJson(dummyResponse));
              break;
            case "GET_ADMIN_DASHBOARD_STATS":
              handleGetAdminDashboardStats(request);
              break;
            case "LEAVE_ROOM":
              handleLeaveRoom(request);
              break;
            //            case "LEAVE_ROOM":
            //              // Gửi cho con Zombie 1 cục xương để nó nhả hàm readLine() ra
            //              Response leaveRes = new Response("LEAVE_SUCCESS", "Giải phóng luồng
            // thành công");
            //              sendMessage(gson.toJson(leaveRes));
            //              break;
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

  // LẤY DỮ LIỆU TỔNG HỢP CHO TRANG DASHBOARD ADMIN
  private void handleGetAdminDashboardStats(Request request) {
    try {
      Map<String, String> kpis = org.example.server.daos.DashboardDAO.getInstance().getKPIs();
      Map<String, Integer> categories = org.example.server.daos.DashboardDAO.getInstance().getCategoryDistribution();
      Map<String, Integer> auctionStatus = org.example.server.daos.DashboardDAO.getInstance().getAuctionStatusDistribution();
      org.example.core.dto.admin.AdminDashboardDTO dashboardDTO =
              new org.example.core.dto.admin.AdminDashboardDTO(kpis, categories, auctionStatus);
      Response response = new Response("SUCCESS", "Lấy dữ liệu Dashboard thành công", dashboardDTO);
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi Server khi lấy số liệu Dashboard: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleRegister(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      RegisterRequestDTO registerRequest = gson.fromJson(dataJson, RegisterRequestDTO.class);

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
      String dataJson = gson.toJson(request.getData());
      LoginRequestDTO loginRequest = gson.fromJson(dataJson, LoginRequestDTO.class);

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
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Server Error: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleDeleteProduct(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      DeleteRequestDTO deleteRequest = gson.fromJson(dataJson, DeleteRequestDTO.class);

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
      CreateAuctionDTO auctionReq = gson.fromJson(dataJson, CreateAuctionDTO.class);

      Auction newAuction = AuctionService.createAuction(auctionReq);
      ItemDAO.getInstance().updateItemStatus(auctionReq.getItem().getItemId(), ItemStatus.LISTED);

      Response response = new Response("SUCCESS", "Đã lên sàn đấu giá thành công!", newAuction);
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi tạo đấu giá: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleJoinRoom(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      JsonObject jsonObject = gson.fromJson(dataJson, JsonObject.class);
      int auctionId = jsonObject.get("auctionId").getAsInt();

      // Ghi nhận phòng hiện tại và đăng ký với Server để nhận Broadcast
      this.currentRoomId = auctionId;
      AuctionServer.addClientToRoom(auctionId, this);

      Response response = new Response("SUCCESS", "Đã tham gia phòng " + auctionId);
      sendMessage(gson.toJson(response));
    } catch (Exception e) {
      Response response = new Response("ERROR", "Lỗi khi join phòng: " + e.getMessage());
      sendMessage(gson.toJson(response));
    }
  }

  private void handleLeaveRoom(Request request) {
    // Xóa client khỏi danh sách nhận Broadcast của phòng đó
    if (this.currentRoomId != -1) {
      AuctionServer.removeClientFromRoom(this.currentRoomId, this);
      this.currentRoomId = -1;
    }

    Response leaveRes = new Response("LEAVE_SUCCESS", "Giải phóng luồng thành công");
    sendMessage(gson.toJson(leaveRes));
  }

//  private void handleApproveAuction(Request request) {
//    try {
//      String dataJson = gson.toJson(request.getData());
//      org.example.core.dto.admin.AdminApproveAuctionDTO approveReq =
//              gson.fromJson(dataJson, org.example.core.dto.admin.AdminApproveAuctionDTO.class);
//
//      User requester = userDAO.getUserByUserId(approveReq.getAdminId());
//      if (requester == null || requester.getRole() != RoleType.ADMIN) {
//        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
//        return;
//      }
//
//      AuctionService.approveAuction(approveReq.getAuctionId());
//
//      Response response = new Response("SUCCESS", "Đã duyệt và hẹn giờ mở cửa phiên " + approveReq.getAuctionId());
//      sendMessage(gson.toJson(response));
//    } catch (Exception e) {
//      e.printStackTrace(); // In lỗi ra console Server để dễ debug nếu có
//      Response response = new Response("ERROR", "Lỗi khi duyệt phiên: " + e.getMessage());
//      sendMessage(gson.toJson(response));
//    }
//  }

  private void handlePlaceBid(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      BidRequestDTO bidReq = gson.fromJson(dataJson, BidRequestDTO.class);

      boolean success = BiddingService.getInstance().placeBid(bidReq);

      if (success) {

        String realUsername = "Unknown";

        try {
          User user = userDAO.getUserByUserId(bidReq.getUserId());

          if (user != null) {
            realUsername = user.getUserName();
          }
        } catch (Exception e) {
          System.out.println("Lỗi truy vấn tên người dùng: " + e.getMessage());
        }
        LocalDateTime currentEndTime = null;

        try {
          Auction updatedAuction =
              AuctionDAO.getInstance().getAuctionByAuctionId(bidReq.getAuctionId());
          if (updatedAuction != null) {
            currentEndTime = updatedAuction.getEndTime();
          }
        } catch (Exception e) {
          System.out.println("Lỗi lấy thời gian kết thúc: " + e.getMessage());
        }

        BidBroadcastDTO broadcastDTO =
            new BidBroadcastDTO(
                bidReq.getAuctionId(),
                bidReq.getBidAmount().doubleValue(),
                realUsername,
                currentEndTime);

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
      String dataJson = gson.toJson(request.getData());
      Integer auctionId = gson.fromJson(dataJson, Integer.class);

      List<BidTransaction> history = BiddingService.getInstance().getBidHistory(auctionId);

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
          userService.balanceDeposit(
              depositRequest.getUserId(), depositRequest.getAmount(), depositRequest.getPassword());

      Response response;
      if (success) {

        java.math.BigDecimal newBalance =
            userDAO.getUserByUserId(depositRequest.getUserId()).getBalance();

        response = new Response("SUCCESS", "Nạp tiền thành công!", newBalance);

      } else {
        response = new Response("ERROR", "Nạp tiền thất bại.");
      }

      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi nạp tiền: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleGetActiveAuctions() {
    try {
      List<Auction> runningAuctions = AuctionService.getAuctionsByStatus(AuctionStatus.RUNNING);

      List<Auction> openAuctions = AuctionService.getAuctionsByStatus(AuctionStatus.OPEN);

      List<Auction> activeItems = new java.util.ArrayList<>();

      if (runningAuctions != null) {
        activeItems.addAll(runningAuctions);
      }
      if (openAuctions != null) {
        activeItems.addAll(openAuctions);
      }

      Response response =
              new Response("SUCCESS", "Lấy danh sách đấu giá (RUNNING & OPEN) thành công", activeItems);

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
      }else {
        // PHẢI CÓ DÒNG NÀY ĐỂ CỨU CLIENT KHỎI BỊ TREO
        System.out.println("=> NÂNG CẤP THẤT BẠI TRONG DB (trả về false)");
        Response response = new Response("ERROR", "Nâng cấp thất bại! (Lỗi từ Database: Không có bản ghi nào được cập nhật)");
        sendMessage(gson.toJson(response));
      }
    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Không thể nâng cấp lên Seller???");
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleGetPendingPayments(Request request) {
    try {
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
    try {
      String dataJson = gson.toJson(request.getData());
      PendingPaymentsDTO pendingPaymentsDTO = gson.fromJson(dataJson, PendingPaymentsDTO.class);
      System.out.println(
          "Thong tin pendingPaymentsDTO "
              + pendingPaymentsDTO.getAuctionId()
              + " "
              + pendingPaymentsDTO.getUserId());
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
      for (Integer x : auctionIds) {
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
      String dataJson = gson.toJson(request.getData());
      Integer userId = gson.fromJson(dataJson, Integer.class);

      List<PaidHistoryDTO> paidHistoryDTO = AuctionService.getAllAuctionsPaid(userId);

      Response response =
          new Response("SUCCESS", "Lấy lịch sử thanh toán thành công", paidHistoryDTO);
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse =
          new Response("ERROR", "Lỗi lấy lịch sử thanh toán: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleAdminProcessItem(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      AdminProcessItemDTO processReq = gson.fromJson(dataJson, AdminProcessItemDTO.class);
      User requester = userDAO.getUserByUserId(processReq.getAdminId());
      if (requester == null || requester.getRole() != RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return;
      }
      Item checkItem = itemDAO.getItemById(processReq.getItemId());
      if (checkItem == null) {
        sendMessage(gson.toJson(new Response("ERROR", "Lỗi: Không tìm thấy tài sản ID = " + processReq.getItemId() + ". Vui lòng kiểm tra lại!")));
        return;
      }
      if (checkItem.getStatus() != ItemStatus.DRAFT) {
        Response errorResponse = new Response("ERROR", "Lỗi: Tài sản này không ở trạng thái Bản Nháp (DRAFT)!");
        sendMessage(gson.toJson(errorResponse));
        return;
      }
      ItemStatus newStatus = processReq.isApproved() ? ItemStatus.APPROVED : ItemStatus.REJECTED;
      boolean success = ItemDAO.getInstance().updateItemStatus(processReq.getItemId(), newStatus);
      if (success) {
        String msg = processReq.isApproved() ? "Đã DUYỆT tài sản thành công!" : "Đã TỪ CHỐI tài sản!";
        Response response = new Response("SUCCESS", msg);
        sendMessage(gson.toJson(response));
      } else {
        Response errorResponse = new Response("ERROR", "Lỗi DB: Không thể cập nhật trạng thái.");
        sendMessage(gson.toJson(errorResponse));
      }
    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi Server: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleAdminGetAllUsers(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      Integer adminId = gson.fromJson(dataJson, Integer.class);

      User requester = userDAO.getUserByUserId(adminId);
      if (requester == null || requester.getRole() != RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return;
      }

      List<User> users = userDAO.getAllUsers();
      Response response = new Response("SUCCESS", "Lấy danh sách User thành công", users);
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse =
          new Response("ERROR", "Lỗi khi lấy danh sách User: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleAdminGetAllAuctions() {
    try{
      List<Auction> pendingAuctions = AuctionService.getAuctionsByStatus(AuctionStatus.PENDING);
      Response response = new Response("SUCCESS", "Lay danh sach auctions thanh cong",pendingAuctions);
      sendMessage(gson.toJson(response));
    } catch (Exception e) {
      e.printStackTrace();
      Response errResponse = new Response("ERROR", "Loi khong the lay duoc danh sach auctions");
      sendMessage(gson.toJson(errResponse));
    }
  }

  private void handleAdminBanUser(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      AdminBanUserDTO banReq = gson.fromJson(dataJson, AdminBanUserDTO.class);

      User requester = userDAO.getUserByUserId(banReq.getAdminId());
      if (requester == null || requester.getRole() != RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return;
      }

      boolean success;

      if (banReq.isBanned()) {
        success = userDAO.banStatus(banReq.getUserId());
      } else {
        success = userDAO.unbanStatus(banReq.getUserId());
      }

      if (success) {
        String msg =
            banReq.isBanned() ? "Đã KHÓA tài khoản user thành công!" : "Đã MỞ KHÓA tài khoản user!";
        Response response = new Response("SUCCESS", msg);
        sendMessage(gson.toJson(response));
      } else {
        Response errorResponse =
            new Response("ERROR", "Lỗi DB: Không thể cập nhật trạng thái tài khoản.");
        sendMessage(gson.toJson(errorResponse));
      }
    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi Server: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleGetPendingAuctions(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      Integer adminId = gson.fromJson(dataJson, Integer.class);
      User requester = userDAO.getUserByUserId(adminId);
      if (requester == null || requester.getRole() != RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return;
      }
      List<Auction> pendingAuctions = AuctionService.getAuctionsByStatus(AuctionStatus.PENDING);
      Response response = new Response("SUCCESS", "Lấy danh sách chờ duyệt thành công", pendingAuctions);
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi lấy danh sách PENDING: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleAdminCancelAuction(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      AdminCancelAuctionDTO cancelReq = gson.fromJson(dataJson, AdminCancelAuctionDTO.class);

      User requester = userDAO.getUserByUserId(cancelReq.getAdminId());
      if (requester == null || requester.getRole() != RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return;
      }

      AuctionService.forceCancelAuction(cancelReq.getAuctionId(), "Admin hủy khẩn cấp");

      Response response = new Response("SUCCESS", "Đã HỦY KHẨN CẤP phiên đấu giá!");
      sendMessage(gson.toJson(response));

      Response broadcast =
          new Response("AUCTION_END", "Phiên đấu giá bị Admin hủy bỏ khẩn cấp!", "ADMIN_CANCELLED");
      broadcastMessage(gson.toJson(broadcast));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse = new Response("ERROR", "Lỗi Server: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  private void handleAdminGetDaftItems(Request request) {
    try {
      String dataJson = gson.toJson(request.getData());
      Integer adminId = gson.fromJson(dataJson, Integer.class);

      User requester = userDAO.getUserByUserId(adminId);
      if (requester == null || requester.getRole() != RoleType.ADMIN) {
        sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
        return;
      }

      List<Item> daftItems = itemDAO.getItemsByStatus(ItemStatus.DRAFT);

      Response response =
          new Response("SUCCESS", "Lấy danh sách chờ duyệt thành công", daftItems);
      sendMessage(gson.toJson(response));

    } catch (Exception e) {
      e.printStackTrace();
      Response errorResponse =
          new Response("ERROR", "Lỗi Server khi lấy danh sách chờ duyệt: " + e.getMessage());
      sendMessage(gson.toJson(errorResponse));
    }
  }

  public synchronized void sendMessage(String message) {
    out.println(message);
  }

  private void closeConnection() {
    try {
      connectedClients.remove(this);

      if (this.currentRoomId != -1) {
        AuctionServer.removeClientFromRoom(this.currentRoomId, this);
        this.currentRoomId = -1;
      }

      System.out.println(
          "[SERVER] Một Client vừa thoát. Tổng số người online: " + connectedClients.size());

      if (in != null) in.close();
      if (out != null) out.close();
      if (clientSocket != null) clientSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
      Response response = new Response("ERROR", "Loi connection");
    }
  }

  public static void broadcastMessage(String message) {
    for (ClientHandler client : connectedClients) {
      try {
        client.sendMessage(message);
      } catch (Exception e) {
        System.err.println("Lỗi khi gửi Broadcast cho 1 client: " + e.getMessage());
      }
    }
  }
}
