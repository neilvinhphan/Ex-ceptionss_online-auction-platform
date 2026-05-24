package org.example.client.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import org.example.client.network.ClientManager;
import org.example.client.network.GroqAIService;
import org.example.client.utils.AuctionSession;
import org.example.client.utils.ImageUtils;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.entities.Auction;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class MarketHistoryController extends BaseController implements Initializable {

    @FXML private TableView<Auction> tvMarketHistory;
    @FXML private TableColumn<Auction, String> colItemName;
    @FXML private TableColumn<Auction, BigDecimal> colFinalPrice;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private TableColumn<Auction, LocalDateTime> colEndTime;
    @FXML private TableColumn<Auction, Integer> colBidCount;

    // Các biến cho Tab Chi Tiết Nửa Phải
    @FXML private ImageView imgProduct;
    @FXML private Label lblDetailName;
    @FXML private Label lblDetailDesc;
    @FXML private Label lblDetailPrice;
    @FXML private Label lblDetailBids;
    @FXML private Label lblAnalysis;
    @FXML private LineChart<String, Number> chartPriceTrend;

    private ObservableList<Auction> historyList = FXCollections.observableArrayList();
    private final Gson gson = new Gson();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        loadMarketHistory();
        tvMarketHistory.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                updateDetailPanel(newSelection);
            }
        });
    }

    private void setupColumns() {
        // 1. Tên sản phẩm
        colItemName.setCellValueFactory(cellData -> {
            if (cellData.getValue().getItem() != null) {
                return new SimpleStringProperty(cellData.getValue().getItem().getItemName());
            }
            return new SimpleStringProperty("Sản phẩm ẩn");
        });

        // 2. Giá chốt hạ (Custom tô màu Text)
        colFinalPrice.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getHighestBid()));
        colFinalPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%,d VNĐ", price.longValue()));
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #e11d48;"); // Màu đỏ đô nổi bật
                }
            }
        });

        // 3. Trạng thái (Custom Badge màu sắc)
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    badge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 12;");

                    if ("PAID".equals(status)) {
                        badge.setText("ĐÃ THANH TOÁN");
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #dcfce7; -fx-text-fill: #166534;"); // Xanh lá
                    } else if ("FINISHED".equals(status)) {
                        badge.setText("CHỜ THANH TOÁN");
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #fef08a; -fx-text-fill: #854d0e;"); // Vàng nhạt
                    }

                    setGraphic(badge);
                }
            }
        });

        // 4. Ngày đóng phiên
        colEndTime.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getEndTime()));
        colEndTime.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                }
            }
        });

        // 5. Số lượng đặt giá
        colBidCount.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getTotalBids()));
        colBidCount.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
    }

    private void loadMarketHistory() {
        Request request = new Request("GET_MARKET_HISTORY", null);
        Gson gson = ClientManager.getInstance().getGson();

        new Thread(() -> {
            try {
                // 🌟 Sửa getClientSocket() thành getClient() cho đúng tên hàm của ông
                String resJson = ClientManager.getInstance().getClient().sendRequest(gson.toJson(request));
                Response res = gson.fromJson(resJson, Response.class);

                if ("SUCCESS".equals(res.getStatus())) {
                    Type listType = new TypeToken<List<Auction>>(){}.getType();
                    List<Auction> list = gson.fromJson(gson.toJson(res.getData()), listType);

                    Platform.runLater(() -> {
                        historyList.setAll(list);
                        tvMarketHistory.setItems(historyList);
                    });
                }
            } catch (Exception e) {
                System.err.println("Lỗi tải lịch sử thị trường: " + e.getMessage());
            }
        }).start();
    }

    private void updateDetailPanel(Auction selectedAuction) {
        if (selectedAuction.getItem() == null) return;

        // 1. Cập nhật Text & Ảnh
        lblDetailName.setText(selectedAuction.getItem().getItemName());
        lblDetailDesc.setText(selectedAuction.getItem().getDescription());
        lblDetailPrice.setText(String.format("%,d VNĐ", selectedAuction.getHighestBid().longValue()));
        lblDetailBids.setText(selectedAuction.getTotalBids() + " lượt đặt giá");

        // (Nếu ông có thư viện ImageUtils convert Base64 sang Ảnh thì dùng, không thì tạm để trống hoặc load ảnh default)
        try {
            if (selectedAuction.getItem().getImage() != null && !selectedAuction.getItem().getImage().isEmpty()) {
                // Tùy cách ông đang load ảnh ở Catalog mà ốp vào đây nhé
                 imgProduct.setImage(ImageUtils.decodeBase64ToImage(selectedAuction.getItem().getImage()));
            }
        } catch (Exception e) {}

        // =========================================================
        // 2. VẼ BIỂU ĐỒ (Lọc chính xác theo Item ID)
        // =========================================================
        chartPriceTrend.getData().clear(); // Xóa dữ liệu cũ
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Lịch sử giá");

        // 🔥 Lấy ID duy nhất của sản phẩm thay vì lấy Tên
        int targetItemId = selectedAuction.getItem().getItemId();

        // Lọc trong list những phòng đã đóng có cùng ID Sản Phẩm
        historyList.stream()
                .filter(a -> a.getItem() != null && a.getItem().getItemId() == targetItemId) // Fix lọc ở đây
                .sorted((a1, a2) -> a1.getEndTime().compareTo(a2.getEndTime())) // Sắp xếp từ cũ đến mới
                .forEach(a -> {
                    // Trục X là Ngày, Trục Y là Giá
                    // Thêm giờ phút vào format để tránh trùng lặp nếu nó được đấu 2 lần trong cùng 1 ngày
                    String dateLabel = a.getEndTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
                    series.getData().add(new XYChart.Data<>(dateLabel, a.getHighestBid()));
                });

        chartPriceTrend.getData().add(series);

        // =========================================================
        // PHASE 3 & 4: TÍCH HỢP AI PHÂN TÍCH THỊ TRƯỜNG
        // =========================================================

        // Đặt trạng thái Loading cho UI để người dùng biết máy đang nghĩ
        lblAnalysis.setText("⏳ Đang kết nối AI để phân tích dữ liệu...");

        // 1. Gom dữ liệu tạo Prompt (Phase 3 - Prompt Engineering)
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là một chuyên gia phân tích thị trường đấu giá chuyên nghiệp. ");
        prompt.append("Hãy phân tích ngắn gọn, súc tích (tối đa 4 câu) về xu hướng giá cả và mức độ quan tâm của người mua đối với sản phẩm này.\n");
        prompt.append("[THÔNG TIN SẢN PHẨM]\n");
        prompt.append("- Tên: ").append(selectedAuction.getItem().getItemName()).append("\n");
        if (selectedAuction.getItem().getStartingPrice() != null) {
            prompt.append("- Giá khởi điểm gốc: ").append(String.format("%,.0f VNĐ", selectedAuction.getItem().getStartingPrice())).append("\n");
        }

        prompt.append("\n[LỊCH SỬ GIAO DỊCH (Từ cũ đến mới)]\n");

        historyList.stream()
                .filter(a -> a.getItem() != null && a.getItem().getItemId() == targetItemId)
                .sorted((a1, a2) -> a1.getEndTime().compareTo(a2.getEndTime()))
                .forEach(a -> {
                    prompt.append("- Ngày ").append(a.getEndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                            .append(": Chốt ").append(String.format("%,.0f VNĐ", a.getHighestBid()))
                            .append(" | ").append(a.getTotalBids()).append(" lượt thầu.\n");
                });

        // 2. Chạy ngầm gọi API để không làm lag giao diện (Phase 4 - Multi-threading)
        new Thread(() -> {
            try {
                // Gọi API từ class GroqAIService (Ông nhớ import thư viện nếu IDE yêu cầu nhé)
                String aiResponse = GroqAIService.analyzeMarket(prompt.toString());

                // Đẩy kết quả trả về giao diện (Bắt buộc dùng Platform.runLater)
                Platform.runLater(() -> {
                    lblAnalysis.setText(aiResponse);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblAnalysis.setText("❌ Lỗi khi phân tích dữ liệu: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleBackToCatalog(ActionEvent event) {
        try {
            switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục đấu giá VET");
        } catch (Exception e) {
            System.err.println("Lỗi quay lại Catalog: " + e.getMessage());
            e.printStackTrace();
        }
    }
}