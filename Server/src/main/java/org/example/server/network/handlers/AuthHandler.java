package org.example.server.network.handlers;

import com.google.gson.Gson;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.userDTO.LoginRequestDTO;
import org.example.core.dto.userDTO.RegisterRequestDTO;
import org.example.core.exception.AuctionException;
import org.example.core.exception.AuthenticationException;
import org.example.core.exception.DataConflictException;
import org.example.core.models.users.User;
import org.example.server.network.ClientHandler;
import org.example.server.services.AuthService;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthHandler implements RequestHandler {
    private static final Logger logger = Logger.getLogger(AuthHandler.class.getName());
    private final AuthService authService = AuthService.getInstance();
    private final Gson gson;

    public AuthHandler(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void handle(Request request, ClientHandler client) throws Exception {
        switch (request.getAction()) {
            case REGISTER -> handleRegister(request, client);
            case LOGIN -> handleLogin(request, client);
            case LOGOUT -> handleLogout(request, client);
        }
    }

    private void handleRegister(Request request, ClientHandler client) {
        try {
            String dataJson = gson.toJson(request.getData());
            RegisterRequestDTO registerRequest = gson.fromJson(dataJson, RegisterRequestDTO.class);

            User newUser = authService.register(registerRequest);
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Đăng ký tài khoản thành công!", newUser)));

        } catch (AuctionException e) {
            logger.log(Level.WARNING, "Lỗi nghiệp vụ đăng ký: " + e.getMessage());
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi hệ thống trong quá trình đăng ký", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server nội bộ: " + e.getMessage(), 5000)));
        }
    }

    private void handleLogin(Request request, ClientHandler client) {
        try {
            LoginRequestDTO loginRequest = gson.fromJson(gson.toJson(request.getData()), LoginRequestDTO.class);
            User newUser = authService.login(loginRequest);

            if (newUser == null) {
                throw new AuthenticationException("Đăng nhập thất bại: Tài khoản hoặc mật khẩu không hợp lệ.");
            }

            if (ClientHandler.activeUsers.containsKey(newUser.getUserId())) {
                throw new DataConflictException("Tài khoản này đang được đăng nhập ở một thiết bị khác!");
            }

            client.setUserId(newUser.getUserId());
            ClientHandler.activeUsers.put(client.getUserId(), client);
            logger.info("[SERVER] User ID " + client.getUserId() + " đã đăng nhập thành công!!!");
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Đăng nhập thành công!", newUser)));

        } catch (AuctionException e) {
            logger.log(Level.WARNING, "Từ chối đăng nhập nghiệp vụ: " + e.getMessage());
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi xử lý đăng nhập hệ thống", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage(), 5000)));
        }
    }

    private void handleLogout(Request request, ClientHandler client) {
        try {
            if (client.getUserId() != -1) {
                ClientHandler.activeUsers.remove(client.getUserId());
                logger.info("[SERVER] User ID " + client.getUserId() + " đã Đăng xuất an toàn.");
                client.setUserId(-1);
            }
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Đăng xuất thành công!", null)));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi xảy ra trong quá trình xử lý đăng xuất", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi Server: " + e.getMessage(), 5000)));
        }
    }
}