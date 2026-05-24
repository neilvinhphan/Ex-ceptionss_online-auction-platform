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
import org.example.core.shared.enums.AuctionStatus;
import org.example.core.shared.enums.ItemStatus;
import org.example.core.shared.enums.RoleType;

import org.example.core.models.items.Item;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.VehicleItem;
import org.example.core.models.users.User;
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

import org.example.core.network.LocalDateTimeAdapter;

public class ClientHandler implements Runnable {
    public static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

    private static final AuthService authService = AuthService.getInstance();
    private static final UserService userService = UserService.getInstance();
    private static final ItemService itemService = ItemService.getInstance();
    private static final AuctionService auctionService = AuctionService.getInstance();
    private static final BiddingService biddingService = BiddingService.getInstance();
    private static final DashBoardService dashBoardService = DashBoardService.getInstance();
    private static final ConcurrentHashMap<Integer, ClientHandler> activeUsers = new ConcurrentHashMap<>();

    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    private int currentRoomId = -1;

    // 🔥 [CẬP NHẬT CHÍ MẠNG]: Biến session để ghi nhớ ID người dùng của kết nối Socket này
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
                        case "GET_APPROVED_ITEMS":
                            handleGetApprovedItems(request);
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
                        case "JOIN_ROOM":
                            handleJoinRoom(request);
                            break;
                        case "APPROVE_AUCTION":
                            break;
                        case "GET_PROMOTED_AUCTIONS":
                            Response dummyResponse = new Response("SUCCESS", "Chưa có dữ liệu", null);
                            sendMessage(gson.toJson(dummyResponse));
                            break;
                        case "GET_ADMIN_DASHBOARD_STATS":
                            handleGetAdminDashboardStats(request);
                            break;
                        case "GET_SELLER_DASHBOARD":
                            handleGetSellerDashboard(request);
                            break;
                        case "LEAVE_ROOM":
                            handleLeaveRoom(request);
                            break;
                        case "REGISTER_AUTOBID":
                            handleRegisterAutoBid(request);
                            break;
                        case "CANCEL_AUTOBID":
                            handleCancelAutoBid(request);
                            break;
                        case "GET_MARKET_HISTORY":
                            handleGetMarketHistory(request);
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

    private void handleGetSellerDashboard(Request request) {
        try {
            String dataJson = gson.toJson(request.getData());
            Integer sellerId = gson.fromJson(dataJson, Integer.class);

            SellerDashboardDTO dto = dashBoardService.getSellerDashboard(sellerId);

            Response response = new Response("SUCCESS", "Lấy dữ liệu thành công", dto);
            sendMessage(gson.toJson(response));
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage())));
        }
    }

    private void handleGetAdminDashboardStats(Request request) {
        try {
            Map<String, String> kpis = dashBoardService.getKPIs();
            Map<String, Integer> categories = dashBoardService.getCategoryDistribution();
            Map<String, Integer> auctionStatus = dashBoardService.getAuctionStatusDistribution();
            AdminDashboardDTO dashboardDTO =
                    new AdminDashboardDTO(kpis, categories, auctionStatus);
            Response response = new Response("SUCCESS", "Lấy dữ liệu Dashboard thành công", dashboardDTO);
            sendMessage(gson.toJson(response));

        } catch (Exception e) {
            e.printStackTrace();
            Response errorResponse =
                    new Response("ERROR", "Lỗi Server khi lấy số liệu Dashboard: " + e.getMessage());
            sendMessage(gson.toJson(errorResponse));
        }
    }

    private void handleRegister(Request request) {
        try {
            String dataJson = gson.toJson(request.getData());
            RegisterRequestDTO registerRequest = gson.fromJson(dataJson, RegisterRequestDTO.class);

            User newUser = authService.register(registerRequest);

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

            User newUser = authService.login(loginRequest);
            Response response;
            if (newUser != null) {
                // 🔥 CHỐT CHẶN: Kiểm tra xem user này có đang online ở máy khác không?
                if (activeUsers.containsKey(newUser.getUserId())) {
                    sendMessage(gson.toJson(new Response("ERROR", "Tài khoản này đang được đăng nhập ở một thiết bị khác!", null)));
                    return;
                }

                this.userId = newUser.getUserId();
                activeUsers.put(this.userId, this); // Ghi danh vào sổ online

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

            Item newItem = itemService.createItem(finalDTO);

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

            boolean success = itemService.deleteItem(deleteRequest);
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
        try {
            String dataJson = gson.toJson(request.getData());
            EditProductRequestDTO editRequest = gson.fromJson(dataJson, EditProductRequestDTO.class);
            boolean success = itemService.updateItemFull(editRequest);
            Response response;
            if (success) {
                Item item = itemService.getItemById(editRequest.getItemId());
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
            List<Item> items = itemService.getAllItem(pendingRequest);
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

            Auction newAuction = auctionService.createAuction(auctionReq);
            itemService.updateItemStatus(auctionReq.getItem().getItemId(), ItemStatus.LISTED);

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

            this.currentRoomId = auctionId;
            AuctionServer.addClientToRoom(auctionId, this);

            Response response = new Response("SUCCESS", "Đã tham gia phòng " + auctionId);
            sendMessage(gson.toJson(response));

            if (this.userId != -1) {
                try {
                    BigDecimal savedMaxBid = biddingService.getMaxAutoBid(auctionId, this.userId);

                    if (savedMaxBid != null) {
                        Map<String, Object> autoBidState = new HashMap<>();
                        autoBidState.put("maxBid", savedMaxBid.doubleValue());

                        Response stateResponse = new Response("MY_AUTOBID_STATUS", "Khôi phục trạng thái Bot", autoBidState);
                        sendMessage(gson.toJson(stateResponse));
                        System.out.println("🤖 [RE-JOIN] Đã gửi gói tin khôi phục trạng thái Bot cho User " + this.userId + " tại phòng " + auctionId);
                    }

                    AuctionServer.broadcastRoomUserCount(auctionId);
                } catch (Exception e) {
                    System.err.println("❌ Lỗi kiểm tra trạng thái AutoBid khi Join Room: " + e.getMessage());
                }
            }

            // 🔥 FIX LỖI VÀO PHÒNG FINISHED: Bốc data mới tinh từ DB gửi riêng cho ông Client này
            try {
                // Dùng AuctionService thay vì AuctionDAO
                org.example.core.models.entities.Auction freshAuction =
                        org.example.server.services.AuctionService.getInstance().getAuctionByAuctionId(auctionId);

                if (freshAuction != null) {
                    if (freshAuction.getBidderId() > 0) {
                        // Dùng UserService thay vì UserDAO
                        org.example.core.models.users.User winner =
                                org.example.server.services.UserService.getInstance().getUserById(freshAuction.getBidderId());

                        if (winner != null) {
                            freshAuction.setHighestBidderName(winner.getUserName());
                        }
                    }
                    sendMessage(gson.toJson(new Response("INITIAL_ROOM_DATA", "Dữ liệu khởi tạo phòng mới nhất", freshAuction)));
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi đồng bộ dữ liệu ban đầu cho Client vào phòng: " + e.getMessage());
            }

        } catch (Exception e) {
            Response response = new Response("ERROR", "Lỗi khi join phòng: " + e.getMessage());
            sendMessage(gson.toJson(response));
        }
    }

    private void handleLeaveRoom(Request request) {
        int oldRoomId = this.currentRoomId;

        if (this.currentRoomId != -1) {
            AuctionServer.removeClientFromRoom(this.currentRoomId, this);
            this.currentRoomId = -1;
        }

        AuctionServer.broadcastRoomUserCount(oldRoomId);

        Response leaveRes = new Response("LEAVE_SUCCESS", "Giải phóng luồng thành công");
        sendMessage(gson.toJson(leaveRes));
    }

    private void handlePlaceBid(Request request) {
        try {
            String dataJson = gson.toJson(request.getData());
            BidRequestDTO bidReq = gson.fromJson(dataJson, BidRequestDTO.class);
            String realUsername = bidReq.getUserName();

            boolean success = biddingService.placeBid(bidReq);

            if (success) {
                LocalDateTime currentEndTime = null;
                try {
                    Auction updatedAuction = auctionService.getAuctionById(bidReq.getAuctionId());
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

                biddingService.evaluateDeterministicBidding(bidReq.getAuctionId());
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

            List<BidTransaction> history = biddingService.getBidHistory(auctionId);

            Response response = new Response("SUCCESS", "Lấy lịch sử thành công", history);
            sendMessage(gson.toJson(response));

        } catch (Exception e) {
            e.printStackTrace();
            Response errorResponse = new Response("ERROR", "Lỗi lấy lịch sử: " + e.getMessage());
            sendMessage(gson.toJson(errorResponse));
        }
    }

    private void handleDeposit(Request request) {
        try {
            String dataJson = gson.toJson(request.getData());
            DepositRequestDTO depositRequest = gson.fromJson(dataJson, DepositRequestDTO.class);

            BigDecimal newBalance =
                    userService.balanceDeposit(
                            depositRequest.getUserId(), depositRequest.getAmount(), depositRequest.getPassword());

            Response response;
            if (newBalance != null) {
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
            List<Auction> runningAuctions = auctionService.getAuctionsByStatus(AuctionStatus.RUNNING);
            List<Auction> openAuctions = auctionService.getAuctionsByStatus(AuctionStatus.OPEN);
            List<Auction> finishedAuctions = auctionService.getAuctionsByStatus(AuctionStatus.FINISHED);

            List<Auction> activeItems = new ArrayList<>();

            if (runningAuctions != null) {
                activeItems.addAll(runningAuctions);
            }
            if (openAuctions != null) {
                activeItems.addAll(openAuctions);
            }

            Response response =
                    new Response("SUCCESS", "Lấy danh sách đấu giá đang diễn ra thành công", activeItems);
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
            UpdateRoleRequestDTO requestDTO = gson.fromJson(dataJson, UpdateRoleRequestDTO.class);

            boolean success = userService.updateRole(requestDTO.getUserId());
            if (success) {
                Response response = new Response("SUCCESS", "Đã nâng cấp lên Seller thành công!!!!");
                sendMessage(gson.toJson(response));
            } else {
                System.out.println("=> NÂNG CẤP THẤT BẠI TRONG DB (trả về false)");
                Response response =
                        new Response(
                                "ERROR",
                                "Nâng cấp thất bại! (Lỗi từ Database: Không có bản ghi nào được cập nhật)");
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
            List<PendingPaymentsDTO> pendingPaymentsDTOS = auctionService.getAllAuctionsFinished(userId);
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
            int auctionId = pendingPaymentsDTO.getAuctionId();
            int bidderId = pendingPaymentsDTO.getUserId();
            boolean success = auctionService.checkoutAuction(auctionId, bidderId);
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
            List<Integer> auctionIds = auctionService.getAllItemPaidPending(userId);
            for (Integer x : auctionIds) {
                auctionService.checkoutAuction(x, userId);
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

            List<PaidHistoryDTO> paidHistoryDTO = auctionService.getAllAuctionsPaid(userId);

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
            Item checkItem = itemService.getItemById(processReq.getItemId());
            if (checkItem == null) {
                sendMessage(
                        gson.toJson(
                                new Response(
                                        "ERROR",
                                        "Lỗi: Không tìm thấy tài sản ID = "
                                                + processReq.getItemId()
                                                + ". Vui lòng kiểm tra lại!")));
                return;
            }
            if (checkItem.getStatus() != ItemStatus.PENDING) {
                Response errorResponse =
                        new Response("ERROR", "Lỗi: Tài sản này không ở trạng thái PENDING)!");
                sendMessage(gson.toJson(errorResponse));
                return;
            }
            ItemStatus newStatus = processReq.isApproved() ? ItemStatus.APPROVED : ItemStatus.REJECTED;
            boolean success = itemService.updateItemStatus(processReq.getItemId(), newStatus);
            if (success) {
                String msg =
                        processReq.isApproved() ? "Đã DUYỆT tài sản thành công!" : "Đã TỪ CHỐI tài sản!";
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

            User requester = userService.getUserById(adminId);
            if (requester == null || requester.getRole() != RoleType.ADMIN) {
                sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
                return;
            }

            List<User> users = UserService.getInstance().getAllUsers();
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
        try {
            List<Auction> allAuctions = new java.util.ArrayList<>();
            for (AuctionStatus status : AuctionStatus.values()) {
                List<Auction> listByStatus = auctionService.getAuctionsByStatus(status);
                if (listByStatus != null) {
                    allAuctions.addAll(listByStatus);
                }
            }
            Response response = new Response("SUCCESS", "Lấy toàn bộ danh sách Auctions thành công", allAuctions);
            sendMessage(gson.toJson(response));

        } catch (Exception e) {
            e.printStackTrace();
            Response errResponse = new Response("ERROR", "Lỗi không thể lấy được danh sách auctions: " + e.getMessage());
            sendMessage(gson.toJson(errResponse));
        }
    }

    private void handleAdminBanUser(Request request) {
        try {
            String dataJson = gson.toJson(request.getData());
            AdminBanUserDTO banReq = gson.fromJson(dataJson, AdminBanUserDTO.class);

            User requester = userService.getUserById(banReq.getAdminId());
            if (requester == null || requester.getRole() != RoleType.ADMIN) {
                sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
                return;
            }

            boolean success;
            if (banReq.isBanned()) {
                success = userService.banUser(banReq.getUserId());
            } else {
                success = userService.unbanUser(banReq.getUserId());
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

    private void handleAdminCancelAuction(Request request) {
        try {
            String dataJson = gson.toJson(request.getData());
            Integer auctionId = gson.fromJson(dataJson, Integer.class);

            auctionService.forceCancelAuction(auctionId, "Admin hủy khẩn cấp");

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

    private void handleAdminGetPendingItems(Request request) {
        try {
            String dataJson = gson.toJson(request.getData());
            Integer adminId = gson.fromJson(dataJson, Integer.class);

            User requester = userService.getUserById(adminId);
            if (requester == null || requester.getRole() != RoleType.ADMIN) {
                sendMessage(gson.toJson(new Response("ERROR", "Báo động: Mày không phải Admin!")));
                return;
            }

            List<Item> pendingItems = itemService.getAllItemByStatus(ItemStatus.PENDING);

            Response response = new Response("SUCCESS", "Lấy danh sách chờ duyệt thành công", pendingItems);
            sendMessage(gson.toJson(response));

        } catch (Exception e) {
            e.printStackTrace();
            Response errorResponse =
                    new Response("ERROR", "Lỗi Server khi lấy danh sách chờ duyệt: " + e.getMessage());
            sendMessage(gson.toJson(errorResponse));
        }
    }

    private void handleGetApprovedItems(Request request) {
        try {
            org.example.core.dto.itemsDTO.PendingItemsDTO dto =
                    gson.fromJson(gson.toJson(request.getData()), PendingItemsDTO.class);

            List<Item> approvedItems = itemService.getApprovedItemsByUserId(dto.getSellerId());

            Response response =
                    new Response("SUCCESS", "Tải danh sách sản phẩm thành công", approvedItems);
            sendMessage(gson.toJson(response));

        } catch (Exception e) {
            e.printStackTrace();
            Response errorResponse =
                    new Response("ERROR", "Không thể tải danh sách tài sản: " + e.getMessage());
            sendMessage(gson.toJson(errorResponse));
        }
    }

    private void handleRegisterAutoBid(Request request) {
        try {
            AutoBidRequestDTO regDto = gson.fromJson(
                    gson.toJson(request.getData()), AutoBidRequestDTO.class);

            biddingService.saveOrUpdateAutoBid(
                    regDto.getAuctionId(), regDto.getUserId(), regDto.getMaxBid());

            sendMessage(gson.toJson(new Response("SUCCESS", "Kích hoạt hệ thống AutoBid gác phòng thành công!", null)));

            biddingService.evaluateDeterministicBidding(regDto.getAuctionId());
        } catch (Exception e) {
            sendMessage(gson.toJson(new Response("ERROR", "Lỗi kích hoạt AutoBid: " + e.getMessage(), null)));
        }
    }

    private void handleCancelAutoBid(Request request) {
        try {
            AutoBidRequestDTO cancelDto = gson.fromJson(
                    gson.toJson(request.getData()), AutoBidRequestDTO.class);

            biddingService.disableAutoBid(
                    cancelDto.getAuctionId(), cancelDto.getUserId());

            sendMessage(gson.toJson(new Response("SUCCESS", "Đã hủy hệ thống tự động trả giá thành công!", null)));
        } catch (Exception e) {
            sendMessage(gson.toJson(new Response("ERROR", "Lỗi hủy AutoBid: " + e.getMessage(), null)));
        }
    }

    private void handleGetMarketHistory(Request request) {
        try {
            List<Auction> history = auctionService.getMarketHistory();
            sendMessage(gson.toJson(new Response("SUCCESS", "Dữ liệu lịch sử thị trường", history)));
        } catch (Exception e) {
            sendMessage(gson.toJson(new Response("ERROR", "Lỗi lấy lịch sử thị trường: " + e.getMessage(), null)));
        }
    }

    public synchronized void sendMessage(String message) {
        out.println(message);
    }

    private void closeConnection() {
        try {
            // 🔥 GẠCH TÊN KHỎI SỔ KHI CLIENT NGẮT KẾT NỐI (Tắt app, mất mạng, đăng xuất...)
            if (this.userId != -1) {
                activeUsers.remove(this.userId);
                System.out.println("[SERVER] User ID " + this.userId + " đã offline và được xóa khỏi danh sách.");
                this.userId = -1;
            }

            connectedClients.remove(this);
            int oldRoomId = this.currentRoomId;

            if (this.currentRoomId != -1) {
                AuctionServer.removeClientFromRoom(this.currentRoomId, this);
                this.currentRoomId = -1;
            }

            AuctionServer.broadcastRoomUserCount(oldRoomId);

            System.out.println(
                    "[SERVER] Một Client vừa thoát. Tổng số người online: " + connectedClients.size());

            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
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