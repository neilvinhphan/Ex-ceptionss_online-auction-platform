package org.example.client.controllers.auction;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.example.client.controllers.BaseController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.AuctionSession;
import org.example.client.utils.UserSession;
import org.example.core.dto.auctionDTO.CreateAuctionDTO;
import org.example.core.dto.itemsDTO.PendingItemsDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.entities.Auction;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;
import org.example.core.shared.enums.ItemStatus;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import javafx.scene.control.DatePicker;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Controller chịu trách nhiệm điều khiển màn hình tạo mới phòng đấu giá dành cho Seller. Hỗ trợ
 * đồng bộ tự động điền danh mục, giá sàn từ danh sách tài sản đã được Admin phê duyệt (APPROVED).
 */
public class CreateAuctionController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(CreateAuctionController.class.getName());

  @FXML private Spinner<Integer> durationHourSpinner;
  @FXML private Spinner<Integer> durationMinuteSpinner;
  @FXML private ComboBox<Item> cbPendingItems;
  @FXML private ComboBox<String> cbCategory;
  @FXML private TextField tfStartingPrice;
  @FXML private TextField tfBidIncrement;
  @FXML private DatePicker dpStartDate;
  @FXML private Spinner<Integer> startHourSpinner;
  @FXML private Spinner<Integer> startMinuteSpinner;

  private final List<Item> allPendingItems = new ArrayList<>();
  private boolean isAutoSelecting = false;
  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  /**
   * Khởi tạo giá trị mặc định cho các Spinner thời gian, định dạng ComboBox và nạp tài sản khả
   * dụng.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    initSpinners();
    setupItemDisplayFormat();
    loadPendingItems();
    setupListeners();

    SpinnerValueFactory<Integer> startHourFactory =
        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, LocalDateTime.now().getHour());
    startHourSpinner.setValueFactory(startHourFactory);
    startHourSpinner.setEditable(true);

    SpinnerValueFactory<Integer> startMinuteFactory =
        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, LocalDateTime.now().getMinute());
    startMinuteSpinner.setValueFactory(startMinuteFactory);
    startMinuteSpinner.setEditable(true);

    dpStartDate.setValue(LocalDate.now());
  }

  /**
   * Xử lý đóng gói DTO và submit yêu cầu tạo mới phòng đấu giá lên Server khi nhấn nút xác nhận.
   */
  @FXML
  public void handleSubmit(ActionEvent event) {
    Item selectedItem = cbPendingItems.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      showAlert("Lỗi", "Vui lòng chọn một tài sản để tạo đấu giá!");
      return;
    }
    String bidIncrText = tfBidIncrement.getText().trim();
    if (bidIncrText.isEmpty()) {
      showAlert("Lỗi", "Vui lòng nhập bước giá!");
      return;
    }

    LocalDate startDate = dpStartDate.getValue();
    if (startDate == null) {
      showAlert("Lỗi", "Vui lòng chọn ngày mở phiên đấu giá!");
      return;
    }
    int startHour = startHourSpinner.getValue();
    int startMinute = startMinuteSpinner.getValue();
    LocalDateTime startTime = LocalDateTime.of(startDate, LocalTime.of(startHour, startMinute));

    if (startTime.isBefore(LocalDateTime.now())) {
      showAlert("Lỗi", "Thời gian mở phòng dự kiến phải lớn hơn thời gian hiện tại!");
      return;
    }

    if (getDuration().toMinutes() <= 0) {
      showAlert("Lỗi", "Thời gian đấu giá phải lớn hơn 0!");
      return;
    }

    try {
      long durationMinutes = getDuration().toMinutes();
      BigDecimal bidIncrement = new BigDecimal(tfBidIncrement.getText().trim());

      CreateAuctionDTO requestDTO =
          new CreateAuctionDTO(selectedItem, durationMinutes, bidIncrement, startTime);
      Request request = new Request("CREATE_AUCTION", requestDTO);
      String jsonRequest = gson.toJson(request);

      new Thread(
              () -> {
                try {
                  String jsonResponse = clientSocket.sendRequest(jsonRequest);
                  Response response = gson.fromJson(jsonResponse, Response.class);

                  Platform.runLater(
                      () -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                          String jsonData = gson.toJson(response.getData());
                          Auction newAuction = gson.fromJson(jsonData, Auction.class);

                          AuctionSession.getInstance().setCurrentAuction(newAuction);
                          AuctionSession.getInstance().setCurrentItem(selectedItem);

                          AuctionSession.getInstance().clearSession();
                          switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục đấu giá");
                        } else {
                          showAlert("Lỗi tạo đấu giá", response.getMessage());
                        }
                      });
                } catch (Exception e) {
                  Platform.runLater(
                      () -> showAlert("Lỗi kết nối", "Không thể gửi yêu cầu: " + e.getMessage()));
                  logger.log(
                      Level.SEVERE, "Gặp lỗi kết nối khi thực hiện bắn gói tin tạo đấu giá", e);
                }
              })
          .start();

      logger.log(
          Level.INFO,
          "Đã đóng gói CreateAuctionDTO thành công cho tài sản: {0}",
          selectedItem.getItemName());

    } catch (Exception e) {
      showAlert("Lỗi hệ thống", "Có lỗi xảy ra: " + e.getMessage());
      logger.log(Level.SEVERE, "Lỗi hệ thống trong hàm xử lý Submit tạo đấu giá", e);
    }
  }

  /** Tải danh sách vật phẩm đang thuộc trạng thái APPROVED từ server về để đưa vào ComboBox. */
  private void loadPendingItems() {
    int sellerId = UserSession.getInstance().getCurrentUser().getUserId();
    PendingItemsDTO requestPayload = new PendingItemsDTO(sellerId);
    requestPayload.setSellerId(sellerId);

    try {
      Request request = new Request("GET_APPROVED_ITEMS", requestPayload);
      String jsonRequest = gson.toJson(request);

      new Thread(
              () -> {
                try {
                  String jsonResponse = clientSocket.sendRequest(jsonRequest);
                  Response response = gson.fromJson(jsonResponse, Response.class);

                  Platform.runLater(
                      () -> {
                        if (response.getStatus().equals("SUCCESS")) {
                          String jsonData = gson.toJson(response.getData());
                          JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
                          allPendingItems.clear();

                          for (JsonElement element : jsonArray) {
                            JsonObject itemObj = element.getAsJsonObject();
                            String type = itemObj.get("type").getAsString();
                            Item parsedItem = null;

                            switch (type.toUpperCase()) {
                              case "ART" -> parsedItem = gson.fromJson(itemObj, ArtItem.class);
                              case "ELECTRONICS" ->
                                  parsedItem = gson.fromJson(itemObj, ElectronicsItem.class);
                              case "VEHICLE" ->
                                  parsedItem = gson.fromJson(itemObj, VehicleItem.class);
                              default ->
                                  logger.log(
                                      Level.WARNING,
                                      "Không xác định được loại item type: {0}",
                                      type);
                            }

                            if (parsedItem != null
                                && parsedItem.getStatus() == ItemStatus.APPROVED) {
                              allPendingItems.add(parsedItem);
                            }
                          }
                          cbPendingItems.setItems(
                              FXCollections.observableArrayList(allPendingItems));
                        } else {
                          showAlert("Lỗi", response.getMessage());
                        }
                      });
                } catch (Exception e) {
                  Platform.runLater(
                      () ->
                          showAlert(
                              "Lỗi kết nối", "Không thể kết nối đến server: " + e.getMessage()));
                  logger.log(Level.SEVERE, "Lỗi luồng mạng khi nạp danh sách Approved Items", e);
                }
              })
          .start();
    } catch (Exception e) {
      showAlert("Lỗi", "Có lỗi xảy ra khi gửi yêu cầu: " + e.getMessage());
      logger.log(Level.SEVERE, "Lỗi khởi tạo yêu cầu nạp danh sách Approved Items", e);
    }
  }

  /** Lắp đặt các bộ lắng nghe (Listeners) để xử lý logic tự điền giá và lọc phân loại. */
  private void setupListeners() {
    cbCategory
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (isAutoSelecting || newValue == null) return;

              ObservableList<Item> filteredList;
              if (newValue.equals("Tất cả") || newValue.equals("-- Tất cả danh mục --")) {
                filteredList = FXCollections.observableArrayList(allPendingItems);
              } else {
                List<Item> filtered =
                    allPendingItems.stream()
                        .filter(item -> newValue.equals(item.getType()))
                        .collect(Collectors.toList());
                filteredList = FXCollections.observableArrayList(filtered);
              }
              cbPendingItems.setItems(filteredList);
              cbPendingItems.getSelectionModel().clearSelection();
              tfStartingPrice.clear();
            });

    cbPendingItems
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, oldValue, selectedItem) -> {
              if (selectedItem == null) {
                tfStartingPrice.clear();
                cbCategory.getSelectionModel().clearSelection();
                return;
              }
              isAutoSelecting = true;
              cbCategory.setValue(selectedItem.getType());
              if (selectedItem.getStartingPrice() != null) {
                tfStartingPrice.setText(selectedItem.getStartingPrice().toString());
              }
              isAutoSelecting = false;
            });
  }

  /** Format giao diện hiển thị chuỗi tên của sản phẩm thay vì in mã băm memory address. */
  private void setupItemDisplayFormat() {
    cbPendingItems.setConverter(
        new StringConverter<Item>() {
          @Override
          public String toString(Item item) {
            return item == null ? "" : item.getItemName();
          }

          @Override
          public Item fromString(String string) {
            return null;
          }
        });
  }

  private void initSpinners() {
    durationHourSpinner.setValueFactory(
        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 72, 1));
    durationMinuteSpinner.setValueFactory(
        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
    durationHourSpinner.setEditable(true);
    durationMinuteSpinner.setEditable(true);
  }

  private Duration getDuration() {
    int hours = (durationHourSpinner.getValue() != null) ? durationHourSpinner.getValue() : 0;
    int minutes = (durationMinuteSpinner.getValue() != null) ? durationMinuteSpinner.getValue() : 0;
    return Duration.ofHours(hours).plusMinutes(minutes);
  }
}
