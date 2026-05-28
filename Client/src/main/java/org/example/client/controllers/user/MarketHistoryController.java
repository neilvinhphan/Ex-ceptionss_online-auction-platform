package org.example.client.controllers.user;

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

import org.example.client.controllers.BaseController;
import org.example.client.network.ClientManager;
import org.example.client.network.GroqAIService;
import org.example.client.utils.ImageUtils;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.entities.Auction;
import org.example.core.shared.enums.ActionType;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller xử lý màn hình Lịch sử thị trường (Market History). Tích hợp hiển thị danh sách các
 * phiên đấu giá đã kết thúc, vẽ biểu đồ xu hướng giá và kết nối dịch vụ AI (GroqAIService) chạy đa
 * luồng để phân tích biến động thị trường.
 */
public class MarketHistoryController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(MarketHistoryController.class.getName());

  @FXML private TableView<Auction> tvMarketHistory;
  @FXML private TableColumn<Auction, String> colItemName;
  @FXML private TableColumn<Auction, BigDecimal> colFinalPrice;
  @FXML private TableColumn<Auction, String> colStatus;
  @FXML private TableColumn<Auction, LocalDateTime> colEndTime;
  @FXML private TableColumn<Auction, Integer> colBidCount;

  @FXML private ImageView imgProduct;
  @FXML private Label lblDetailName;
  @FXML private Label lblDetailDesc;
  @FXML private Label lblDetailPrice;
  @FXML private Label lblDetailBids;
  @FXML private Label lblAnalysis;
  @FXML private LineChart<String, Number> chartPriceTrend;

  private final ObservableList<Auction> historyList = FXCollections.observableArrayList();
  private final Gson gson = ClientManager.getInstance().getGson();

  /**
   * Khởi tạo giao diện, cấu hình các cột TableView, tải dữ liệu lịch sử và thiết lập bộ lắng nghe
   * chọn dòng.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    setupColumns();
    loadMarketHistory();
    tvMarketHistory
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (obs, oldSelection, newSelection) -> {
              if (newSelection != null) {
                updateDetailPanel(newSelection);
              }
            });
  }

  /** Xử lý quay lại giao diện danh mục sảnh đấu giá chính (Catalog). */
  @FXML
  private void handleBackToCatalog(ActionEvent event) {
    try {
      switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục đấu giá VET");
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi xảy ra khi quay lại sảnh đấu giá từ màn hình lịch sử", e);
    }
  }

  /**
   * Cấu hình định dạng hiển thị, gán thuộc tính và render badge màu sắc cho các cột trong
   * TableView.
   */
  private void setupColumns() {
    colItemName.setCellValueFactory(
        cellData -> {
          if (cellData.getValue().getItem() != null) {
            return new SimpleStringProperty(cellData.getValue().getItem().getItemName());
          }
          return new SimpleStringProperty("Sản phẩm ẩn");
        });

    colFinalPrice.setCellValueFactory(
        cellData -> new SimpleObjectProperty<>(cellData.getValue().getHighestBid()));
    colFinalPrice.setCellFactory(
        tc ->
            new TableCell<>() {
              @Override
              protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                  setText(null);
                  setStyle("");
                } else {
                  setText(String.format("%,d VNĐ", price.longValue()));
                  setStyle("-fx-font-weight: bold; -fx-text-fill: #e11d48;");
                }
              }
            });

    colStatus.setCellValueFactory(
        cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));
    colStatus.setCellFactory(
        tc ->
            new TableCell<>() {
              @Override
              protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                  setGraphic(null);
                } else {
                  Label badge = new Label(status);
                  badge.setStyle(
                      "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 12;");

                  if ("PAID".equals(status)) {
                    badge.setText("ĐÃ THANH TOÁN");
                    badge.setStyle(
                        badge.getStyle()
                            + "-fx-background-color: #dcfce7; -fx-text-fill: #166534;");
                  } else if ("FINISHED".equals(status)) {
                    badge.setText("CHỜ THANH TOÁN");
                    badge.setStyle(
                        badge.getStyle()
                            + "-fx-background-color: #fef08a; -fx-text-fill: #854d0e;");
                  }
                  setGraphic(badge);
                }
              }
            });

    colEndTime.setCellValueFactory(
        cellData -> new SimpleObjectProperty<>(cellData.getValue().getEndTime()));
    colEndTime.setCellFactory(
        tc ->
            new TableCell<>() {
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

    colBidCount.setCellValueFactory(
        cellData -> new SimpleObjectProperty<>(cellData.getValue().getTotalBids()));
    colBidCount.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
  }

  /**
   * Gửi yêu cầu Socket lên Server để tải toàn bộ lịch sử các phiên đấu giá đã bế mạc trên thị
   * trường.
   */
  private void loadMarketHistory() {
      Request request = new Request(ActionType.GET_MARKET_HISTORY, null);

      new Thread(() -> {
          try {
              String resJson = ClientManager.getInstance().getClient().sendRequest(gson.toJson(request));
              Response res = gson.fromJson(resJson, Response.class);

              if ("SUCCESS".equals(res.getStatus())) {
                  Type listType = new TypeToken<List<Auction>>() {}.getType();
                  List<Auction> list = gson.fromJson(gson.toJson(res.getData()), listType);
                  Platform.runLater(() -> {
                      historyList.setAll(list);
                      tvMarketHistory.setItems(historyList);
                  });
              } else {
                  int code = res.getData() instanceof Number ? ((Number) res.getData()).intValue() : -1;
                  Platform.runLater(() -> showAlert("Lỗi tải lịch sử (" + code + ")", res.getMessage()));
              }
          } catch (Exception e) {
              logger.log(Level.SEVERE, "Gặp sự cố kết nối khi gửi lệnh GET_MARKET_HISTORY lên máy chủ", e);
          }
      }).start();
  }

  /**
   * Cập nhật thông tin chi tiết của phòng đấu giá được chọn, lọc vẽ lại biểu đồ đường và gửi dữ
   * liệu prompt lên AI phân tích.
   */
  private void updateDetailPanel(Auction selectedAuction) {
    if (selectedAuction.getItem() == null) return;

    lblDetailName.setText(selectedAuction.getItem().getItemName());
    lblDetailDesc.setText(selectedAuction.getItem().getDescription());
    lblDetailPrice.setText(String.format("%,d VNĐ", selectedAuction.getHighestBid().longValue()));
    lblDetailBids.setText(selectedAuction.getTotalBids() + " lượt đặt giá");

    try {
      if (selectedAuction.getItem().getImage() != null
          && !selectedAuction.getItem().getImage().isEmpty()) {
        imgProduct.setImage(ImageUtils.decodeBase64ToImage(selectedAuction.getItem().getImage()));
      }
    } catch (Exception e) {
      logger.log(
          Level.WARNING, "Không thể giải mã hình ảnh Base64 cho khung chi tiết lịch sử tài sản", e);
    }

    chartPriceTrend.getData().clear();
    XYChart.Series<String, Number> series = new XYChart.Series<>();
    series.setName("Lịch sử giá");

    int targetItemId = selectedAuction.getItem().getItemId();

    historyList.stream()
        .filter(a -> a.getItem() != null && a.getItem().getItemId() == targetItemId)
        .sorted((a1, a2) -> a1.getEndTime().compareTo(a2.getEndTime()))
        .forEach(
            a -> {
              String dateLabel = a.getEndTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
              series.getData().add(new XYChart.Data<>(dateLabel, a.getHighestBid()));
            });

    chartPriceTrend.getData().add(series);

    lblAnalysis.setText("⏳ Đang kết nối AI để phân tích dữ liệu...");

    StringBuilder prompt = new StringBuilder();
    prompt.append("Bạn là một chuyên gia phân tích thị trường đấu giá chuyên nghiệp. ");
    prompt.append(
        "Hãy phân tích ngắn gọn, súc tích (tối đa 4 câu) về xu hướng giá cả và mức độ quan tâm của người mua đối với sản phẩm này.\n");
    prompt.append("[THÔNG TIN SẢN PHẨM]\n");
    prompt.append("- Tên: ").append(selectedAuction.getItem().getItemName()).append("\n");
    if (selectedAuction.getItem().getStartingPrice() != null) {
      prompt
          .append("- Giá khởi điểm gốc: ")
          .append(String.format("%,.0f VNĐ", selectedAuction.getItem().getStartingPrice()))
          .append("\n");
    }

    prompt.append("\n[LỊCH SỬ GIAO DỊCH (Từ cũ đến mới)]\n");
    historyList.stream()
        .filter(a -> a.getItem() != null && a.getItem().getItemId() == targetItemId)
        .sorted((a1, a2) -> a1.getEndTime().compareTo(a2.getEndTime()))
        .forEach(
            a -> {
              prompt
                  .append("- Ngày ")
                  .append(a.getEndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                  .append(": Chốt ")
                  .append(String.format("%,.0f VNĐ", a.getHighestBid()))
                  .append(" | ")
                  .append(a.getTotalBids())
                  .append(" lượt thầu.\n");
            });

    new Thread(
            () -> {
              try {
                String aiResponse = GroqAIService.analyzeMarket(prompt.toString());
                Platform.runLater(() -> lblAnalysis.setText(aiResponse));
              } catch (Exception e) {
                logger.log(
                    Level.SEVERE,
                    "Gặp lỗi khi thực hiện gọi phương thức analyzeMarket của GroqAIService",
                    e);
                Platform.runLater(
                    () -> lblAnalysis.setText("Lỗi khi phân tích dữ liệu: " + e.getMessage()));
              }
            })
        .start();
  }
}
