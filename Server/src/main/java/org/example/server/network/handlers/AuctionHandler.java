package org.example.server.network.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.auctionDTO.CreateAuctionDTO;
import org.example.core.dto.bidDTO.AutoBidRequestDTO;
import org.example.core.dto.bidDTO.BidBroadcastDTO;
import org.example.core.dto.bidDTO.BidRequestDTO;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.models.users.User;
import org.example.core.shared.enums.AuctionStatus;
import org.example.core.shared.enums.ItemStatus;
import org.example.server.network.AuctionServer;
import org.example.server.network.ClientHandler;
import org.example.server.services.AuctionService;
import org.example.server.services.BiddingService;
import org.example.server.services.ItemService;
import org.example.server.services.UserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionHandler implements RequestHandler {
    private static final Logger logger = Logger.getLogger(AuctionHandler.class.getName());
    private final AuctionService auctionService = AuctionService.getInstance();
    private final BiddingService biddingService = BiddingService.getInstance();
    private final ItemService itemService = ItemService.getInstance();
    private final Gson gson;

    public AuctionHandler(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void handle(Request request, ClientHandler client) throws Exception {
        switch (request.getAction()) {
            case CREATE_AUCTION -> handleCreateAuction(request, client);
            case PLACE_BID -> handlePlaceBid(request, client);
            case GET_BID_HISTORY -> handleGetBidHistory(request, client);
            case GET_ACTIVE_AUCTIONS -> handleGetActiveAuctions(client);
            case JOIN_ROOM -> handleJoinRoom(request, client);
            case LEAVE_ROOM -> handleLeaveRoom(client);
            case REGISTER_AUTOBID -> handleRegisterAutoBid(request, client);
            case CANCEL_AUTOBID -> handleCancelAutoBid(request, client);
            case GET_MARKET_HISTORY -> handleGetMarketHistory(client);
        }
    }

    private void handleCreateAuction(Request request, ClientHandler client) {
        try {
            String dataJson = gson.toJson(request.getData());
            CreateAuctionDTO auctionReq = gson.fromJson(dataJson, CreateAuctionDTO.class);

            Auction newAuction = auctionService.createAuction(auctionReq);
            itemService.updateItemStatus(auctionReq.getItem().getItemId(), ItemStatus.LISTED);

            client.sendMessage(gson.toJson(new Response("SUCCESS", "Đã lên sàn đấu giá thành công!", newAuction)));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi đẩy sản phẩm lên sàn đấu giá", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi tạo đấu giá: " + e.getMessage())));
        }
    }

    private void handlePlaceBid(Request request, ClientHandler client) {
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

                BidBroadcastDTO broadcastDTO = new BidBroadcastDTO(
                        bidReq.getAuctionId(),
                        bidReq.getBidAmount().doubleValue(),
                        bidReq.getUserName(),
                        currentEndTime);

                ClientHandler.broadcastMessage(gson.toJson(new Response("NEW_BID", "Có người vừa đặt giá mới", broadcastDTO)));
                biddingService.evaluateDeterministicBidding(bidReq.getAuctionId());
            } else {
                client.sendMessage(gson.toJson(new Response("ERROR_BID", "Đặt giá không thành công.")));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi xử lý đặt thầu trực tiếp từ Client", e);
            client.sendMessage(gson.toJson(new Response("ERROR_BID", e.getMessage())));
        }
    }

    private void handleGetBidHistory(Request request, ClientHandler client) {
        try {
            Integer auctionId = gson.fromJson(gson.toJson(request.getData()), Integer.class);
            List<BidTransaction> history = biddingService.getBidHistory(auctionId);
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Lấy lịch sử thành công", history)));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi truy vấn lịch sử biểu đồ thầu", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi lấy lịch sử: " + e.getMessage())));
        }
    }

    private void handleGetActiveAuctions(ClientHandler client) {
        try {
            List<Auction> activeItems = new ArrayList<>();
            List<Auction> runningAuctions = auctionService.getAuctionsByStatus(AuctionStatus.RUNNING);
            List<Auction> openAuctions = auctionService.getAuctionsByStatus(AuctionStatus.OPEN);

            if (runningAuctions != null) activeItems.addAll(runningAuctions);
            if (openAuctions != null) activeItems.addAll(openAuctions);

            client.sendMessage(gson.toJson(new Response("SUCCESS", "Lấy danh sách đấu giá đang diễn ra thành công", activeItems)));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi tải dữ liệu các phiên active ngoài sảnh chính", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi khi lấy danh sách đấu giá: " + e.getMessage())));
        }
    }

    private void handleJoinRoom(Request request, ClientHandler client) {
        try {
            JsonObject jsonObject = gson.fromJson(gson.toJson(request.getData()), JsonObject.class);
            int auctionId = jsonObject.get("auctionId").getAsInt();

            client.setCurrentRoomId(auctionId);
            AuctionServer.addClientToRoom(auctionId, client);
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Đã tham gia phòng " + auctionId)));

            if (client.getUserId() != -1) {
                BigDecimal savedMaxBid = biddingService.getMaxAutoBid(auctionId, client.getUserId());
                if (savedMaxBid != null) {
                    Map<String, Object> autoBidState = new HashMap<>();
                    autoBidState.put("maxBid", savedMaxBid.doubleValue());
                    client.sendMessage(gson.toJson(new Response("MY_AUTOBID_STATUS", "Khôi phục trạng thái Bot", autoBidState)));
                    logger.info("[RE-JOIN] Đã gửi gói khôi phục trạng thái Bot cho User " + client.getUserId() + " tại phòng " + auctionId);
                }
                AuctionServer.broadcastRoomUserCount(auctionId);
            }

            Auction freshAuction = AuctionService.getInstance().getAuctionByAuctionId(auctionId);
            if (freshAuction != null) {
                if (freshAuction.getBidderId() > 0) {
                    User winner = UserService.getInstance().getUserById(freshAuction.getBidderId());
                    if (winner != null) freshAuction.setHighestBidderName(winner.getUserName());
                }
                client.sendMessage(gson.toJson(new Response("INITIAL_ROOM_DATA", "Dữ liệu khởi tạo phòng mới nhất", freshAuction)));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi xử lý gia nhập phòng", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi khi join phòng: " + e.getMessage())));
        }
    }

    private void handleLeaveRoom(ClientHandler client) {
        int oldRoomId = client.getCurrentRoomId();
        if (oldRoomId != -1) {
            AuctionServer.removeClientFromRoom(oldRoomId, client);
            client.setCurrentRoomId(-1);
        }
        AuctionServer.broadcastRoomUserCount(oldRoomId);
        client.sendMessage(gson.toJson(new Response("LEAVE_SUCCESS", "Giải phóng luồng thành công")));
    }

    private void handleRegisterAutoBid(Request request, ClientHandler client) {
        try {
            AutoBidRequestDTO regDto = gson.fromJson(gson.toJson(request.getData()), AutoBidRequestDTO.class);
            biddingService.saveOrUpdateAutoBid(regDto.getAuctionId(), regDto.getUserId(), regDto.getMaxBid());

            client.sendMessage(gson.toJson(new Response("SUCCESS", "Kích hoạt hệ thống AutoBid gác phòng thành công!", null)));
            biddingService.evaluateDeterministicBidding(regDto.getAuctionId());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi kích hoạt Bot AutoBid", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi kích hoạt AutoBid: " + e.getMessage(), null)));
        }
    }

    private void handleCancelAutoBid(Request request, ClientHandler client) {
        try {
            AutoBidRequestDTO cancelDto = gson.fromJson(gson.toJson(request.getData()), AutoBidRequestDTO.class);
            biddingService.disableAutoBid(cancelDto.getAuctionId(), cancelDto.getUserId());
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Đã hủy hệ thống tự động trả giá thành công!", null)));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi hủy chế độ đặt giá tự động", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi hủy AutoBid: " + e.getMessage(), null)));
        }
    }

    private void handleGetMarketHistory(ClientHandler client) {
        try {
            List<Auction> history = auctionService.getMarketHistory();
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Dữ liệu lịch sử thị trường", history)));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi trích xuất Market History cho catalog kết quả", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Lỗi lấy lịch sử thị trường: " + e.getMessage(), null)));
        }
    }
}