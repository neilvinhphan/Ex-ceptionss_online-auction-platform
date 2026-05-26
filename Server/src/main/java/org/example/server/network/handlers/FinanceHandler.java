package org.example.server.network.handlers;

import com.google.gson.Gson;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.paymentDTO.PaidHistoryDTO;
import org.example.core.dto.paymentDTO.PendingPaymentsDTO;
import org.example.core.dto.userDTO.DepositRequestDTO;
import org.example.core.exception.AuctionException;
import org.example.core.exception.DataConflictException;
import org.example.core.exception.InvalidUserDataException;
import org.example.server.network.ClientHandler;
import org.example.server.services.AuctionService;
import org.example.server.services.UserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FinanceHandler implements RequestHandler {
    private static final Logger logger = Logger.getLogger(FinanceHandler.class.getName());
    private final UserService userService = UserService.getInstance();
    private final AuctionService auctionService = AuctionService.getInstance();
    private final Gson gson;

    public FinanceHandler(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void handle(Request request, ClientHandler client) throws Exception {
        switch (request.getAction()) {
            case DEPOSIT -> handleDeposit(request, client);
            case GET_PENDING_PAYMENTS -> handleGetPendingPayments(request, client);
            case PAY_ITEM -> handlePayItem(request, client);
            case PAY_ALL -> handlePayAllItems(request, client);
            case GET_PAID_HISTORY -> handleGetPaidHistory(request, client);
        }
    }

    private void handleDeposit(Request request, ClientHandler client) {
        try {
            String dataJson = gson.toJson(request.getData());
            DepositRequestDTO depositRequest = gson.fromJson(dataJson, DepositRequestDTO.class);

            BigDecimal newBalance = userService.balanceDeposit(depositRequest.getUserId(), depositRequest.getAmount(), depositRequest.getPassword());

            if (newBalance == null) {
                throw new InvalidUserDataException("Nạp tiền thất bại. Sai mật khẩu hoặc lỗi hệ thống.");
            }
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Nạp tiền thành công!", newBalance)));

        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi xử lý nạp ví điện tử", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi nạp tiền: " + e.getMessage(), 5000)));
        }
    }

    private void handleGetPendingPayments(Request request, ClientHandler client) {
        try {
            int targetUserId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
            List<PendingPaymentsDTO> pendingPaymentsDTOS = auctionService.getAllAuctionsFinished(targetUserId);
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Thành công!!!", pendingPaymentsDTOS)));

        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi nạp danh sách hóa đơn pending", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Không thể lấy dữ liệu", 5000)));
        }
    }

    private void handlePayItem(Request request, ClientHandler client) {
        try {
            PendingPaymentsDTO pendingPaymentsDTO = gson.fromJson(gson.toJson(request.getData()), PendingPaymentsDTO.class);
            boolean success = auctionService.checkoutAuction(pendingPaymentsDTO.getAuctionId(), pendingPaymentsDTO.getUserId());

            if (!success) {
                throw new DataConflictException("Không thể thanh toán!!! Kiểm tra lại số dư ví hoặc trạng thái hóa đơn.");
            }
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Thanh toán thành công!!!")));

        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi giao dịch thanh toán đơn lẻ hóa đơn", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi giao dịch", 5000)));
        }
    }

    private void handlePayAllItems(Request request, ClientHandler client) {
        try {
            int targetUserId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
            List<Integer> auctionIds = auctionService.getAllItemPaidPending(targetUserId);

            for (Integer x : auctionIds) {
                auctionService.checkoutAuction(x, targetUserId);
            }
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Thanh toán toàn bộ thành công!!!")));

        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi xử lý thanh toán thanh lý hàng loạt hóa đơn", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Thanh toán không thành công", 5000)));
        }
    }

    private void handleGetPaidHistory(Request request, ClientHandler client) {
        try {
            Integer targetUserId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
            List<PaidHistoryDTO> paidHistoryDTO = auctionService.getAllAuctionsPaid(targetUserId);
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Lấy lịch sử thanh toán thành công", paidHistoryDTO)));

        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi lấy nhật ký giao dịch chi tiền ví", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi lấy lịch sử thanh toán: " + e.getMessage(), 5000)));
        }
    }
}