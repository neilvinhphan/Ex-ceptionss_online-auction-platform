package org.example.client.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
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
import org.example.core.dto.PaidHistoryDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AuctionHistoryController extends BaseController implements Initializable {
    @FXML private TableView<PaidHistoryDTO> tvAuctionHistory;
    @FXML private TableColumn<PaidHistoryDTO, String> colItemName;
    @FXML private TableColumn<PaidHistoryDTO, String> colCategory;
    @FXML private TableColumn<PaidHistoryDTO, BigDecimal> colFinalPrice;
    @FXML private TableColumn<PaidHistoryDTO, LocalDateTime> colDate;
    @FXML private MenuButton menuUser;

    private final AuctionClient clientSocket = ClientManager.getInstance().getClient();
    private final Gson gson = ClientManager.getInstance().getGson();
    private final ObservableList<PaidHistoryDTO> historyData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (UserSession.getInstance().getCurrentUser() != null) {
            menuUser.setText(UserSession.getInstance().getCurrentUser().getUserName());
        }
        setupColumns();
        loadHistory();
    }

    private void setupColumns() {
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        // Format tiền VNĐ
        colFinalPrice.setCellValueFactory(new PropertyValueFactory<>("finalPrice"));
        colFinalPrice.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("%,.0f VNĐ", price));
            }
        });
        // Format ngày tháng
        colDate.setCellValueFactory(new PropertyValueFactory<>("paidDate"));
        colDate.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
        });

        tvAuctionHistory.setItems(historyData);
    }

    private void loadHistory() {
        int userId = UserSession.getInstance().getCurrentUser().getUserId();
        Request request = new Request("GET_PAID_HISTORY", userId);

        new Thread(() -> {
            try {
                String resJson = clientSocket.sendRequest(gson.toJson(request));
                Response res = gson.fromJson(resJson, Response.class);
                if ("SUCCESS".equals(res.getStatus())) {
                    Type listType = new TypeToken<List<PaidHistoryDTO>>(){}.getType();
                    List<PaidHistoryDTO> list = gson.fromJson(gson.toJson(res.getData()), listType);
                    Platform.runLater(() -> historyData.setAll(list));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML void handleMain(ActionEvent e) { switchScene(e, "/views/MainView.fxml", "Trang chủ"); }
    @FXML void handleWaitPayment(ActionEvent e) { switchScene(e, "/views/WaitPaymentView.fxml", "Thanh toán"); }
    @FXML void handleWareHouse(ActionEvent e) { switchScene(e, "/views/WareHouseView.fxml", "Kho hàng"); }
    @FXML void handleCreateItem(ActionEvent e) { switchScene(e, "/views/CreateItemView.fxml", "Tạo vật phẩm"); }
    @FXML void handleCreateAuction(ActionEvent e) { switchScene(e, "/views/CreateAuctionView.fxml", "Tạo đấu giá"); }
    @FXML void handleUserUi(ActionEvent e) { switchScene(e, "/views/PersonalView.fxml", "Hồ sơ"); }
    @FXML void handleMenuItem(ActionEvent e) { switchScene(e, "/views/AuctionCatalogView.fxml", "Đấu giá"); }
    @FXML void handleLogout(ActionEvent e) { switchScene(e, "/views/LoginView.fxml", "Đăng nhập"); }
}