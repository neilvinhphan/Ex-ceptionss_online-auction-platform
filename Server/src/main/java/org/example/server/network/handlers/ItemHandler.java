package org.example.server.network.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.itemsDTO.*;
import org.example.core.exception.AuctionException;
import org.example.core.exception.DatabaseAccessException;
import org.example.core.exception.DataConflictException;
import org.example.core.models.items.Item;
import org.example.server.network.ClientHandler;
import org.example.server.services.ItemService;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemHandler implements RequestHandler {
    private static final Logger logger = Logger.getLogger(ItemHandler.class.getName());
    private final ItemService itemService = ItemService.getInstance();
    private final Gson gson;

    public ItemHandler(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void handle(Request request, ClientHandler client) throws Exception {
        switch (request.getAction()) {
            case CREATE_ITEM -> handleCreateItem(request, client);
            case UPDATE_ITEM_FULL -> handleEditProduct(request, client);
            case DELETE_ITEM -> handleDeleteProduct(request, client);
            case GET_PENDING_ITEMS -> handleGetPendingItems(request, client);
            case GET_APPROVED_ITEMS -> handleGetApprovedItems(request, client);
        }
    }

    private void handleCreateItem(Request request, ClientHandler client) {
        try {
            String rawDataJson = gson.toJson(request.getData());
            JsonObject jsonObject = gson.fromJson(rawDataJson, JsonObject.class);
            String type = jsonObject.get("type").getAsString();

            CreateItemRequestDTO finalDTO = switch (type.toUpperCase()) {
                case "ART" -> gson.fromJson(rawDataJson, CreateArtItemDTO.class);
                case "VEHICLE" -> gson.fromJson(rawDataJson, CreateVehicleItemDTO.class);
                case "ELECTRONICS" -> gson.fromJson(rawDataJson, CreateElectronicsItemDTO.class);
                default -> gson.fromJson(rawDataJson, CreateItemRequestDTO.class);
            };

            Item newItem = itemService.createItem(finalDTO);
            if (newItem == null) {
                throw new DatabaseAccessException("Thêm tài sản thất bại do lỗi phía Database.", null);
            }
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Item created successfully!", newItem)));

        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi đăng ký sản phẩm mới", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Server Error: " + e.getMessage(), 5000)));
        }
    }

    private void handleEditProduct(Request request, ClientHandler client) {
        try {
            String dataJson = gson.toJson(request.getData());
            EditProductRequestDTO editRequest = gson.fromJson(dataJson, EditProductRequestDTO.class);

            if (!itemService.updateItemFull(editRequest)) {
                throw new DataConflictException("Cập nhật thất bại. Dữ liệu tài sản không hợp lệ.");
            }

            Item item = itemService.getItemById(editRequest.getItemId());
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Item updated successfully!", item)));

        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật chỉnh sửa sản phẩm", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Server Error: " + e.getMessage(), 5000)));
        }
    }

    private void handleDeleteProduct(Request request, ClientHandler client) {
        try {
            String dataJson = gson.toJson(request.getData());
            DeleteRequestDTO deleteRequest = gson.fromJson(dataJson, DeleteRequestDTO.class);

            if (!itemService.deleteItem(deleteRequest)) {
                throw new DataConflictException("Xóa tài sản thất bại.");
            }
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Item deleted successfully.")));

        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi xóa bỏ sản phẩm", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Server Error: " + e.getMessage(), 5000)));
        }
    }

    private void handleGetPendingItems(Request request, ClientHandler client) {
        try {
            PendingItemsDTO pendingRequest = (request.getData() instanceof PendingItemsDTO dto) ? dto
                    : gson.fromJson(gson.toJson(request.getData()), PendingItemsDTO.class);

            List<Item> items = itemService.getAllItem(pendingRequest);
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Fetched pending items successfully!", items)));

        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi trích xuất kho đồ chờ duyệt của user", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Failed to fetch pending items: " + e.getMessage(), 5000)));
        }
    }

    private void handleGetApprovedItems(Request request, ClientHandler client) {
        try {
            PendingItemsDTO dto = gson.fromJson(gson.toJson(request.getData()), PendingItemsDTO.class);
            List<Item> approvedItems = itemService.getApprovedItemsByUserId(dto.getSellerId());
            client.sendMessage(gson.toJson(new Response("SUCCESS", "Tải danh sách sản phẩm thành công", approvedItems)));

        } catch (AuctionException e) {
            client.sendMessage(gson.toJson(new Response("ERROR", e.getMessage(), e.getErrorCode())));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi trích xuất kho hàng khả dụng ra sàn", e);
            client.sendMessage(gson.toJson(new Response("ERROR", "Không thể tải danh sách tài sản: " + e.getMessage(), 5000)));
        }
    }
}