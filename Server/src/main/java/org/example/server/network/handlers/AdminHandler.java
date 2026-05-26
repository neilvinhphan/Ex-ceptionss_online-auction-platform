package org.example.server.network.handlers;

import com.google.gson.Gson;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.admin.AdminBanUserDTO;
import org.example.core.dto.admin.AdminDashboardDTO;
import org.example.core.dto.admin.AdminProcessItemDTO;
import org.example.core.exception.AuctionException;
import org.example.core.exception.BusinessLogicException;
import org.example.core.exception.ItemNotFoundException;
import org.example.core.exception.UnauthorizedException;
import org.example.core.models.entities.Auction;
import org.example.core.models.items.Item;
import org.example.core.models.users.User;
import org.example.core.shared.enums.AuctionStatus;
import org.example.core.shared.enums.ItemStatus;
import org.example.core.shared.enums.RoleType;
import org.example.server.network.ClientHandler;
import org.example.server.services.AuctionService;
import org.example.server.services.DashBoardService;
import org.example.server.services.ItemService;
import org.example.server.services.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminHandler implements RequestHandler {
    private static final Logger logger = Logger.getLogger(AdminHandler.class.getName());
    private final UserService userService = UserService.getInstance();
    private final ItemService itemService = ItemService.getInstance();
    private final AuctionService auctionService = AuctionService.getInstance();
    private final DashBoardService dashBoardService = DashBoardService.getInstance();
    private final Gson gson;

    public AdminHandler(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void handle(Request request, ClientHandler client) throws Exception {
        switch (request.getAction()) {
            case GET_ADMIN_DASHBOARD_STATS -> handleGetAdminDashboardStats(request, client);
            case ADMIN_PROCESS_ITEM -> handleAdminProcessItem(request, client);
            case ADMIN_GET_ALL_PENDING_ITEMS -> handleAdminGetPendingItems(request, client);
            case ADMIN_GET_ALL_USERS -> handleAdminGetAllUsers(request, client);
            case ADMIN_BAN_USER -> handleAdminBanUser(request, client);
            case ADMIN_CANCEL_AUCTION -> handleAdminCancelAuction(request, client);
            case ADMIN_GET_ALL_AUCTIONS -> handleAdminGetAllAuctions(client);
        }
    }

    private void validateAdmin(Integer adminId) {
        User requester = userService.getUserById(adminId);
        if (requester == null || requester.getRole() != RoleType.ADMIN) {
            throw new UnauthorizedException("Truy cập bị từ chối: Bạn không có quyền Admin!");
        }
    }

    private void handleGetAdminDashboardStats(Request request, ClientHandler client) {
        try {
            AdminDashboardDTO dashboardDTO = new AdminDashboardDTO(
                    dashBoardService.getKPIs(),
                    dashBoardService.getCategoryDistribution(),
                    dashBoardService.getAuctionStatusDistribution());
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Lấy dữ liệu Dashboard thành công", dashboardDTO)));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi kết xuất KPI hệ thống của Admin", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage(), 5000)));
        }
    }

    private void handleAdminProcessItem(Request request, ClientHandler client) {
        try {
            AdminProcessItemDTO processReq = gson.fromJson(gson.toJson(request.getData()), AdminProcessItemDTO.class);
            Item checkItem = itemService.getItemById(processReq.getItemId());

            if (checkItem == null) {
                throw new ItemNotFoundException("Không tìm thấy tài sản ID = " + processReq.getItemId());
            }
            if (checkItem.getStatus() != ItemStatus.PENDING) {
                throw new BusinessLogicException("Tài sản này không ở trạng thái PENDING!");
            }

            ItemStatus newStatus = processReq.isApproved() ? ItemStatus.APPROVED : ItemStatus.REJECTED;
            if (itemService.updateItemStatus(processReq.getItemId(), newStatus)) {
                String msg = processReq.isApproved() ? "Đã DUYỆT tài sản thành công!" : "Đã TỪ CHỐI tài sản!";
                client.sendMessage(gson.toJson(new Response("SUCCESS", msg)));
            } else {
                throw new BusinessLogicException("Lỗi DB: Không thể cập nhật trạng thái.");
            }
        } catch (AuctionException e) {
            logger.log(Level.WARNING, "Lỗi nghiệp vụ phê duyệt tài sản: " + e.getMessage());
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi kiểm duyệt phê duyệt tài sản", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage(), 5000)));
        }
    }

    private void handleAdminGetPendingItems(Request request, ClientHandler client) {
        try {
            Integer adminId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
            validateAdmin(adminId);

            List<Item> pendingItems = itemService.getAllItemByStatus(ItemStatus.PENDING);
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Lấy danh sách chờ duyệt thành công", pendingItems)));
        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi trích xuất kho hàng xếp hàng chờ duyệt", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage(), 5000)));
        }
    }

    private void handleAdminGetAllUsers(Request request, ClientHandler client) {
        try {
            Integer adminId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
            validateAdmin(adminId);

            List<User> users = UserService.getInstance().getAllUsers();
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Lấy danh sách User thành công", users)));
        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi phân hệ quản lý người dùng", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage(), 5000)));
        }
    }

    private void handleAdminBanUser(Request request, ClientHandler client) {
        try {
            AdminBanUserDTO banReq = gson.fromJson(gson.toJson(request.getData()), AdminBanUserDTO.class);
            validateAdmin(banReq.getAdminId());

            boolean success = banReq.isBanned()
                    ? userService.banUser(banReq.getUserId())
                    : userService.unbanUser(banReq.getUserId());
            if (success) {
                String msg = banReq.isBanned() ? "Đã KHÓA tài khoản user thành công!" : "Đã MỞ KHÓA tài khoản user!";
                client.sendMessage(gson.toJson(new Response("SUCCESS", msg)));
            } else {
                throw new BusinessLogicException("Không thể cập nhật trạng thái tài khoản. User có thể không tồn tại.");
            }
        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi thực thi lệnh ban/unban người dùng", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage(), 5000)));
        }
    }

    private void handleAdminCancelAuction(Request request, ClientHandler client) {
        try {
            Integer auctionId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
            auctionService.forceCancelAuction(auctionId, "Admin hủy khẩn cấp");

            client.sendMessage(gson.toJson(new Response("SUCCESS", "Đã HỦY KHẨN CẤP phiên đấu giá!")));
            ClientHandler.broadcastMessage(gson.toJson(new Response("AUCTION_END", "Phiên đấu giá bị Admin hủy bỏ khẩn cấp!", "ADMIN_CANCELLED")));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi thực thi hủy phòng khẩn cấp từ Admin", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage(), 5000)));
        }
    }

    private void handleAdminGetAllAuctions(ClientHandler client) {
        try {
            List<Auction> allAuctions = new ArrayList<>();
            for (AuctionStatus status : AuctionStatus.values()) {
                List<Auction> listByStatus = auctionService.getAuctionsByStatus(status);
                if (listByStatus != null) allAuctions.addAll(listByStatus);
            }
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Lấy toàn bộ danh sách Auctions thành công", allAuctions)));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi tải toàn bộ kho dữ liệu phòng đấu giá cho Admin", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage(), 5000)));
        }
    }
}