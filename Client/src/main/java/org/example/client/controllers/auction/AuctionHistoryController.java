package org.example.client.controllers.auction;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import org.example.client.controllers.BaseController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.paymentDTO.PaidHistoryDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.shared.enums.ActionType;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller xử lý màn hình xem lại lịch sử đấu giá thành công và đã hoàn tất thanh toán. Hiển thị
 * thông tin minh bạch dạng bảng (TableView) kèm định dạng tiền tệ Việt Nam Đồng.
 */
public class AuctionHistoryController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(AuctionHistoryController.class.getName());

  @FXML private TableView<PaidHistoryDTO> tvAuctionHistory;
  @FXML private TableColumn<PaidHistoryDTO, String> colItemName;
  @FXML private TableColumn<PaidHistoryDTO, String> colCategory;
  @FXML private TableColumn<PaidHistoryDTO, BigDecimal> colFinalPrice;
  @FXML private TableColumn<PaidHistoryDTO, LocalDateTime> colDate;

  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();
  private final Gson gson = ClientManager.getInstance().getGson();
  private final ObservableList<PaidHistoryDTO> historyData = FXCollections.observableArrayList();

  /** Phương thức khởi tạo cấu trúc các cột trong bảng dữ liệu lịch sử đấu giá. */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    setupColumns();
    loadHistory();
  }

  /**
   * Cài đặt liên kết thuộc tính DTO vào bảng dữ liệu cùng bộ định dạng giao diện hiển thị cho ngày
   * tháng và giá tiền.
   */
  private void setupColumns() {
    colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
    colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));

    colFinalPrice.setCellValueFactory(new PropertyValueFactory<>("finalPrice"));
    colFinalPrice.setCellFactory(
        tc ->
            new TableCell<>() {
              @Override
              protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if(empty || price == null) {
                    setText(null);
                } else {
                    DecimalFormat formatter = new DecimalFormat("#,###", new DecimalFormatSymbols(new Locale("vi", "VN")));
                    setText(formatter.format(price) + " VNĐ");
                }
              }
            });

    colDate.setCellValueFactory(new PropertyValueFactory<>("paidDate"));
    colDate.setCellFactory(
        tc ->
            new TableCell<>() {
              @Override
              protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                setText(
                    empty || date == null
                        ? null
                        : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
              }
            });

    tvAuctionHistory.setItems(historyData);
  }

  /**
   * Thực hiện tạo tiến trình ngầm gọi Socket gửi mã ID người dùng để lấy dữ liệu lịch sử từ máy
   * chủ.
   */
  private void loadHistory() {
      int userId = UserSession.getInstance().getCurrentUser().getUserId();
      Request request = new Request(ActionType.GET_PAID_HISTORY, userId);

      new Thread(() -> {
          try {
              String resJson = clientSocket.sendRequest(gson.toJson(request));
              Response res = gson.fromJson(resJson, Response.class);

              Platform.runLater(() -> {
                  if ("SUCCESS".equals(res.getStatus())) {
                      Type listType = new TypeToken<List<PaidHistoryDTO>>() {}.getType();
                      List<PaidHistoryDTO> list = gson.fromJson(gson.toJson(res.getData()), listType);
                      historyData.setAll(list);
                  } else {
                      int code = res.getData() instanceof Number ? ((Number) res.getData()).intValue() : -1;
                      showAlert("Lỗi tải lịch sử (" + code + ")", res.getMessage());
                  }
              });
          } catch (Exception e) {
              logger.log(Level.SEVERE, "Không thể tải thành công lịch sử thanh toán đấu giá từ máy chủ", e);
          }
      }).start();
  }
}
