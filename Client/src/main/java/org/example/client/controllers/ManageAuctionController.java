package org.example.client.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.entities.Auction;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;
import org.example.core.models.users.User;
import org.example.core.shared.enums.AuctionStatus;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ManageAuctionController extends BaseController implements Initializable {

    @FXML private TextField searchField;

    // Gắn class Auction của bạn vào bảng
    @FXML private TableView<Auction> auctionTable;

    @FXML private TableColumn<Auction, Integer> colId;
    @FXML private TableColumn<Auction, String> colItemName;
    @FXML private TableColumn<Auction, String> colSeller;
    @FXML private TableColumn<Auction, String> colStartTime;
    @FXML private TableColumn<Auction, BigDecimal> colCurrentPrice;
    @FXML private TableColumn<Auction, String> colStatus;
    private Gson gson = ClientManager.getInstance().getGson();
    private final AuctionClient clientSocket = ClientManager.getInstance().getClient();
    private ObservableList<Auction> auctionList = FXCollections.observableArrayList(); //danh sách gốc

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        loadAuctionsFromServer();
    }

    private void setupTableColumns() {
        // 1. Cột ID Phiên (lấy thẳng biến "id" trong class Auction)
        colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));

        // 2. Cột Tên sản phẩm: Phải  vào trong Item để lấy ra
        colItemName.setCellValueFactory(cellData -> {
            if (cellData.getValue().getItem() != null) {
                return new SimpleStringProperty(cellData.getValue().getItem().getItemName());
            }
            return new SimpleStringProperty("Sản phẩm bị lỗi");
        });

        // 3. Cột Người bán: Lấy SellerID từ Item
        colSeller.setCellValueFactory(cellData -> {
            if (cellData.getValue().getItem() != null) {
                 return new SimpleStringProperty("sellerID");
            }
            return new SimpleStringProperty("NULL");
        });

        // 4. Cột Giờ bắt đầu: Dùng Formatter để biến LocalDateTime thành chữ cho đẹp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colStartTime.setCellValueFactory(cellData -> {
            if (cellData.getValue().getStartTime() != null) {
                return new SimpleStringProperty(cellData.getValue().getStartTime().format(formatter));
            }
            return new SimpleStringProperty("Chưa có");
        });

        // 5. Cột Giá hiện tại (ánh xạ thẳng biến "highestBid")
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("highestBid"));

        // 6. Cột Trạng thái
        colStatus.setCellValueFactory(cellData -> {
            AuctionStatus statusEnum = cellData.getValue().getStatus();
            return new SimpleStringProperty(statusEnum != null ? statusEnum.name() : "Không xác định");
        });
        auctionTable.setItems(auctionList);
    }

    private void loadAuctionsFromServer() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            showAlert("Lỗi", "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn!");
            return;
        }
        // Tạo gói tin yêu cầu lấy TOÀN BỘ phiên đấu giá (của Admin)
        Request request = new Request("ADMIN_GET_ALL_AUCTIONS", null);
        String jsonRequest = gson.toJson(request);

        new Thread(() -> {
            try {
                System.out.println("Đang lấy danh sách tất cả phiên đấu giá từ Server...");
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);

                if ("SUCCESS".equals(response.getStatus())) {

                    // --- BƯỚC 1: XỬ LÝ DỮ LIỆU ĐA HÌNH (PARSING) Ở LUỒNG CHẠY NGẦM ---
                    String jsonData = gson.toJson(response.getData());
                    JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
                    List<Auction> fetchedAuctions = new ArrayList<>();

                    for (JsonElement element : jsonArray) {
                        JsonObject auctionObj = element.getAsJsonObject();
                        // 1.1 Dịch các trường cơ bản của Auction
                        Auction auction = gson.fromJson(auctionObj, Auction.class);

                        // 1.2 Bóc tách thủ công cục Item bên trong dựa vào biến "type"
                        if (auctionObj.has("item") && !auctionObj.get("item").isJsonNull()) {
                            JsonObject itemObj = auctionObj.getAsJsonObject("item");
                            String type = itemObj.get("type").getAsString();

                            Item parsedItem = switch (type.toUpperCase()) {
                                case "ART" -> gson.fromJson(itemObj, ArtItem.class);
                                case "ELECTRONICS" -> gson.fromJson(itemObj, ElectronicsItem.class);
                                case "VEHICLE" -> gson.fromJson(itemObj, VehicleItem.class);
                                default -> null;
                            };
                            auction.setItem(parsedItem);
                        }
                        fetchedAuctions.add(auction);
                    }
                    // --- BƯỚC 2: ĐẨY DANH SÁCH VỪA BÓC TÁCH VÀO GIAO DIỆN CHÍNH ---
                    Platform.runLater(() -> {
                        auctionList.setAll(fetchedAuctions);
                        System.out.println("Đã tải xong " + fetchedAuctions.size() + " phiên đấu giá vào bảng Quản lý.");
                    });

                } else {
                    Platform.runLater(() -> showAlert("Lỗi tải dữ liệu", response.getMessage()));
                }

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối", "Không thể lấy dữ liệu: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim().toLowerCase();

        if (keyword.isEmpty()) {
            auctionTable.setItems(auctionList);
            return;
        }

        ObservableList<Auction> filteredList = FXCollections.observableArrayList();

        for (Auction auction : auctionList) {
            boolean matchId = String.valueOf(auction.getId()).contains(keyword);
            boolean matchItemName = auction.getItem() != null
                    && auction.getItem().getItemName() != null
                    && auction.getItem().getItemName().toLowerCase().contains(keyword);
            if (matchId || matchItemName) {
                filteredList.add(auction);
            }
        }
        auctionTable.setItems(filteredList);
    }

    @FXML
    public void handleRefresh(ActionEvent event) {
        searchField.clear();
        loadAuctionsFromServer();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    @FXML
    public void handleCancelAuction(ActionEvent event) {
        Auction selectedAuction = auctionTable.getSelectionModel().getSelectedItem();

        if (selectedAuction == null) {
            showAlert("Thông báo", "Vui lòng click chọn một phiên đấu giá trên bảng để hủy!");
            return;
        }

        // Chỉ cho phép hủy nếu phiên đang RUNNING
        if (selectedAuction.getStatus() != AuctionStatus.RUNNING && selectedAuction.getStatus() != null) {
            showAlert("Từ chối", "Chỉ có thể hủy những phiên đang chạy (RUNNING).");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("CẢNH BÁO KHẨN CẤP");
        confirm.setHeaderText("Hủy phiên ID: " + selectedAuction.getId() + " - " + selectedAuction.getItem().getItemName() + "?");
        confirm.setContentText("Hành động này sẽ đóng phiên ngay lập tức. Không thể hoàn tác!");

        confirm.showAndWait().ifPresent(responseBtn -> {
            if (responseBtn == ButtonType.OK) {
                System.out.println("Đang gửi lệnh CANCEL_AUCTION lên Server cho ID: " + selectedAuction.getId());
                Request request = new Request("ADMIN_CANCEL_AUCTION", selectedAuction.getId());
                String jsonRequest = gson.toJson(request);

                new Thread(() -> {
                    try {
                        String jsonResponse = clientSocket.sendRequest(jsonRequest);
                        Response serverResponse = gson.fromJson(jsonResponse, Response.class);

                        Platform.runLater(() -> {
                            if ("SUCCESS".equals(serverResponse.getStatus())) {
                                showAlert("Thành công", "Đã hủy phiên đấu giá thành công!");
                                // Cập nhật lại UI:
                               selectedAuction.setStatus(AuctionStatus.CANCELED);
                                auctionTable.refresh(); // Làm mới giao diện bảng
                            } else {
                                showAlert("Lỗi khi hủy", serverResponse.getMessage());
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
                    }
                }).start();
            }
        });
    }
}