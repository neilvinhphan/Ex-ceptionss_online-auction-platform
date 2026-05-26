package org.example.server.network.handlers;

import com.google.gson.Gson;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.userDTO.SellerDashboardDTO;
import org.example.core.dto.userDTO.UpdateRoleRequestDTO;
import org.example.core.exception.AuctionException;
import org.example.core.exception.DatabaseAccessException;
import org.example.server.network.ClientHandler;
import org.example.server.services.DashBoardService;
import org.example.server.services.UserService;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UserHandler implements RequestHandler {
    private static final Logger logger = Logger.getLogger(UserHandler.class.getName());
    private final UserService userService = UserService.getInstance();
    private final DashBoardService dashBoardService = DashBoardService.getInstance();
    private final Gson gson;

    public UserHandler(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void handle(Request request, ClientHandler client) throws Exception {
        switch (request.getAction()) {
            case UPDATE_ROLE -> handleUpdateRole(request, client);
            case GET_SELLER_DASHBOARD -> handleGetSellerDashboard(request, client);
        }
    }

    private void handleUpdateRole(Request request, ClientHandler clientHandler) {
        try {
            UpdateRoleRequestDTO requestDTO = gson.fromJson(gson.toJson(request.getData()), UpdateRoleRequestDTO.class);

            if (userService.updateRole(requestDTO.getUserId())) {

                clientHandler.forceLogout();

                clientHandler.sendMessage(gson.toJson(new Response("SUCCESS", "Đã nâng cấp lên Seller thành công!")));
            } else {
                clientHandler.sendMessage(gson.toJson(new Response("ERROR", "Nâng cấp thất bại! Lỗi cơ sở dữ liệu.")));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi nâng cấp định danh", e);
            clientHandler.sendMessage(gson.toJson(new Response("ERROR", "Lỗi server.")));
        }
    }

    private void handleGetSellerDashboard(Request request, ClientHandler client) {
        try {
            Integer sellerId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
            SellerDashboardDTO dto = dashBoardService.getSellerDashboard(sellerId);
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Lấy dữ liệu thành công", dto)));

        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi kết xuất thống kê doanh số Seller", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage(), 5000)));
        }
    }
}