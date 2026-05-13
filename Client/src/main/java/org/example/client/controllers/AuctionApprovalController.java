package org.example.client.controllers;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.ImageUtils;
import org.example.client.utils.UserSession;
import org.example.core.dto.PendingAuctionDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.entities.Auction;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AuctionApprovalController extends BaseController implements Initializable {

    @FXML private TableView<Auction> itemTable;
    @FXML private TableColumn<Auction, Integer> colId;
    @FXML private TableColumn<Auction, String> colItemName;
    @FXML private TableColumn<Auction, String> colBidIncrement;
    @FXML private TableColumn<Auction, String> colDuration;

    @FXML private ImageView itemImageView;
    @FXML private Label lblNoImage;
    @FXML private Label lblName;
    @FXML private Label lblType;
    @FXML private Label lblPrice;
    @FXML private TextArea txtDescription;
    @FXML private Button btnApprove;
    @FXML private Button btnReject;

    private ObservableList<Auction> pendingAuctions = FXCollections.observableArrayList();
    private final Gson gson = ClientManager.getInstance().getGson();
    private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        itemTable.setItems(pendingAuctions);
        // Lắng nghe sự kiện chọn dòng trên TableView
        itemTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                displayAuctionDetails(newSelection);
            } else {
                clearDetailsPane();
            }
        });
        loadPendingAuctions();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        // Lấy tên item từ đối tượng Item bên trong Auction
        colItemName.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getItem().getItemName()));

        colBidIncrement.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%,.0f đ", cellData.getValue().getBidIncrement().doubleValue())));

        colDuration.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDurationMinutes() + " phút"));
    }

    private void loadPendingAuctions() {
        PendingAuctionDTO requestPayload = new PendingAuctionDTO(UserSession.getInstance().getCurrentUser().getUserId());
        Request request = new Request("GET_PENDING_AUCTIONS", requestPayload);
        String jsonRequest = gson.toJson(request);

        new Thread(() -> {
            try {
                System.out.println("Đang yêu cầu danh sách Đấu giá chờ duyệt...");
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                System.out.println("DEBUG Dữ liệu đấu giá: " + jsonResponse);
                Response response = gson.fromJson(jsonResponse, Response.class);
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        String jsonData = gson.toJson(response.getData());
                        JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();

                        List<Auction> fetchedList = new ArrayList<>();

                        for (JsonElement element : jsonArray) {
                            JsonObject auctionObj = element.getAsJsonObject();

                            // Lấy object 'item' bên trong để xử lý đa hình
                            JsonObject itemObj = auctionObj.getAsJsonObject("item");
                            String type = itemObj.get("type").getAsString();
                            Item parsedItem = null;

                            switch (type.toUpperCase()) {
                                case "ART" -> parsedItem = gson.fromJson(itemObj, ArtItem.class);
                                case "ELECTRONICS" -> parsedItem = gson.fromJson(itemObj, ElectronicsItem.class);
                                case "VEHICLE" -> parsedItem = gson.fromJson(itemObj, VehicleItem.class);
                                default -> parsedItem = gson.fromJson(itemObj, Item.class);
                            }

                            if (parsedItem != null) {
                                Auction auction = gson.fromJson(auctionObj, Auction.class);
                                auction.setItem(parsedItem); // Gán item đã ép kiểu đúng vào auction
                                fetchedList.add(auction);
                            }
                        }
                        pendingAuctions.setAll(fetchedList);
                        System.out.println("Đã tải xong " + fetchedList.size() + " cuộc đấu giá.");
                    } else {
                        showAlert("Lỗi", response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void displayAuctionDetails(Auction auction) {
        Item item = auction.getItem();
        lblName.setText(item.getItemName());
        lblType.setText(item.getType());
        lblPrice.setText(String.format("%,.0f đ", item.getStartingPrice().doubleValue()));
        txtDescription.setText(item.getDescription());

        // Xử lý ảnh
        if (item.getImage() != null && !item.getImage().isEmpty()) {
            new Thread(() -> {
                Image img = ImageUtils.decodeBase64ToImage(item.getImage());
                Platform.runLater(() -> {
                    itemImageView.setImage(img);
                    lblNoImage.setVisible(img == null);
                });
            }).start();
        } else {
            itemImageView.setImage(null);
            lblNoImage.setVisible(true);
        }

        btnApprove.setDisable(false);
        btnReject.setDisable(false);
    }

    @FXML
    void handleApprove(ActionEvent event) {
        processAction("APPROVE_AUCTION", "phê duyệt");
    }

    @FXML
    void handleReject(ActionEvent event) {
        processAction("REJECT_AUCTION", "từ chối");
    }

    private void processAction(String command, String actionName) {
        Auction selected = itemTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Request request = new Request(command, selected.getAuctionId());
        new Thread(() -> {
            try {
                String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
                Response response = gson.fromJson(jsonResponse, Response.class);

                Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        showAlert("Thành công", "Đã " + actionName + " cuộc đấu giá này.");
                        pendingAuctions.remove(selected);
                        clearDetailsPane();
                    } else {
                        showAlert("Lỗi", response.getMessage());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        loadPendingAuctions();
    }

    @FXML
    void handleBack(ActionEvent event) {
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    private void clearDetailsPane() {
        lblName.setText("...");
        lblType.setText("...");
        lblPrice.setText("...");
        txtDescription.setText("");
        itemImageView.setImage(null);
        lblNoImage.setVisible(true);
        btnApprove.setDisable(true);
        btnReject.setDisable(true);
    }
}