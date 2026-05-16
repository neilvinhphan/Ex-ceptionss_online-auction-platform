package org.example.client.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
import org.example.core.models.users.User;
import org.example.core.shared.enums.ItemStatus;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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

public class CreateAuctionController extends BaseController implements Initializable {
  @FXML private MenuButton menuUser;
  @FXML private Spinner<Integer> durationHourSpinner;
  @FXML private Spinner<Integer> durationMinuteSpinner;
  @FXML private ComboBox<Item> cbPendingItems;
  @FXML private ComboBox<String> cbCategory;
  @FXML private TextField tfStartingPrice;
  @FXML private TextField tfBidIncrement;
  @FXML private DatePicker dpStartDate;
  @FXML private Spinner<Integer> startHourSpinner;
  @FXML private Spinner<Integer> startMinuteSpinner;

  private List<Item> allPendingItems = new ArrayList<>();
  private boolean isAutoSelecting = false;
  private Gson gson = ClientManager.getInstance().getGson();
  private AuctionClient clientSocket = ClientManager.getInstance().getClient();

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    initUser();
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

  private void initUser() {
    User currentUser = UserSession.getInstance().getCurrentUser();
    if (currentUser != null) {
      menuUser.setText(currentUser.getUserName());
    }
  }

  private void initSpinners() {
    durationHourSpinner.setValueFactory(
        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 72, 1));
    durationMinuteSpinner.setValueFactory(
        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
    durationHourSpinner.setEditable(true);
    durationMinuteSpinner.setEditable(true);
  }

  // =========================================================
  // 🔹 XỬ LÝ DỮ LIỆU & LOGIC LỌC / AUTO-FILL
  // =========================================================
  private void loadPendingItems() {
    int sellerId = UserSession.getInstance().getCurrentUser().getUserId();
    PendingItemsDTO requestPayload = new PendingItemsDTO(sellerId);
    requestPayload.setSellerId(sellerId);

    System.out.println("Khởi tạo luồng tải sản phẩm đã duyệt...");
    try {
      // 🛠️ CẬP NHẬT: Đổi tên lệnh thành lệnh lấy đồ APPROVED của riêng User này
      Request request = new Request("GET_APPROVED_ITEMS", requestPayload);
      String jsonRequest = gson.toJson(request);

      new Thread(
              () -> {
                try {
                  System.out.println("Gửi yêu cầu tải danh sách sản phẩm APPROVED...");
                  String jsonResponse = clientSocket.sendRequest(jsonRequest);
                  Response response = gson.fromJson(jsonResponse, Response.class);

                  Platform.runLater(
                      () -> {
                        if ("SUCCESS".equals(response.getStatus())) {
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
                              default -> System.out.println("Unknown item type: " + type);
                            }

                            if (parsedItem != null) {
                              // 🛠️ MÀNG LỌC MỚI: Chỉ nhận những món đồ mang trạng thái APPROVED
                              // mới cho lên sàn
                              if (parsedItem.getStatus() == ItemStatus.APPROVED) {
                                allPendingItems.add(parsedItem);
                              }
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
                              "Lỗi kết nối",
                              "Không thể lấy danh sách sản phẩm: " + e.getMessage()));
                  e.printStackTrace();
                }
              })
          .start();
    } catch (Exception e) {
      showAlert("Lỗi", "Có lỗi xảy ra khi gửi yêu cầu: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void setupItemDisplayFormat() {
    cbPendingItems.setConverter(
        new StringConverter<Item>() {
          @Override
          public String toString(Item item) {
            if (item == null) return "";
            return item.getItemName();
          }

          @Override
          public Item fromString(String string) {
            return null;
          }
        });
  }

  private void setupListeners() {
    // 🎧 LISTENER 1: KHI NGƯỜI DÙNG BẤM CHỌN DANH MỤC (LỌC TÀI SẢN)
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

    // 🎧 LISTENER 2: KHI NGƯỜI DÙNG CHỌN 1 TÀI SẢN (TỰ ĐỘNG ĐIỀN GIÁ AI)
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

              if (selectedItem.getSuggestedPrice() != null) {
                tfStartingPrice.setText(selectedItem.getSuggestedPrice().toString());
                tfStartingPrice.setStyle(
                    "-fx-text-fill: #deff9a; -fx-font-weight: bold; -fx-border-color: #28a745;");
                System.out.println(
                    "🤖 Tự động điền giá gợi ý từ AI cho: " + selectedItem.getItemName());
              } else if (selectedItem.getStartingPrice() != null) {
                tfStartingPrice.setText(selectedItem.getStartingPrice().toString());
                tfStartingPrice.setStyle("-fx-text-fill: white;");
              }

              isAutoSelecting = false;
            });
  }

  // =========================================================
  // 🔹 SUBMIT & ĐIỀU HƯỚNG
  // =========================================================
  public void handleSubmit(ActionEvent event) {
    Item selectedItem = cbPendingItems.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      showAlert("Lỗi", "Vui lòng chọn một tài sản để tạo đấu giá!");
      return;
    }

    // 🛠️ CHỐT CHẶN BẢO MẬT: Phòng hờ lỗi giao diện, chỉ cho phép đồ APPROVED tạo phòng
    if (selectedItem.getStatus() != ItemStatus.APPROVED) {
      showAlert("Lỗi nghiệp vụ", "Vật phẩm này chưa được kiểm duyệt hoàn tất!");
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

                          // 🛠️ SỬA THÔNG BÁO: Phù hợp kịch bản hệ thống tự động chạy ngầm không
                          // qua Admin duyệt phòng nữa
                          showAlert(
                              "Thành công",
                              "Tạo phòng đấu giá thành công! Phiên đấu giá đang ở trạng thái chờ mở cửa (OPEN) và sẽ tự động lên sàn vào lúc "
                                  + startTime.toString());

                          AuctionSession.getInstance().clearSession();

                          switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục đấu giá");

                        } else {
                          showAlert("Lỗi tạo đấu giá", response.getMessage());
                        }
                      });
                } catch (Exception e) {
                  Platform.runLater(
                      () -> showAlert("Lỗi kết nối", "Không thể gửi yêu cầu: " + e.getMessage()));
                  e.printStackTrace();
                }
              })
          .start();

      System.out.println(
          "Đã gửi DTO tạo đấu giá thành công! Tài sản: " + selectedItem.getItemName());

    } catch (Exception e) {
      showAlert("Lỗi hệ thống", "Có lỗi xảy ra: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void handleMain(ActionEvent event) {
    switchScene(event, "/views/MainView.fxml", "Trang chủ");
  }

  public void handleUserUi(ActionEvent event) {
    switchScene(event, "/views/PersonalView.fxml", "Hồ sơ cá nhân");
  }

  public void handleLogout(ActionEvent event) {
    UserSession.getInstance().cleanUserSession();
    switchScene(event, "/views/LoginView.fxml", "Đăng nhập");
  }

  public void handleCreateAuction(ActionEvent event) {
    switchScene(event, "/views/CreateAuctionView.fxml", "Tạo đấu giá");
  }

  @FXML
  void handleWareHouse(ActionEvent event) {
    switchScene(event, "/views/WareHouseView.fxml", "Kho hàng");
  }

  @FXML
  public void handleWaitPayment(ActionEvent event) {
    switchScene(event, "/views/WaitPaymentView.fxml", "Sản phẩm chờ thanh toán");
  }

  @FXML
  void handleCreateItem(ActionEvent event) {
    switchScene(event, "/views/CreateItemView.fxml", "Tạo sản phẩm đấu giá");
  }

  public void handleMenuItem(ActionEvent event) {
    switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục đấu giá");
  }

  private Duration getDuration() {
    int hours = (durationHourSpinner.getValue() != null) ? durationHourSpinner.getValue() : 0;
    int minutes = (durationMinuteSpinner.getValue() != null) ? durationMinuteSpinner.getValue() : 0;
    return Duration.ofHours(hours).plusMinutes(minutes);
  }

  public void handleHistoryAuction(ActionEvent event) {
    switchScene(event, "/views/AuctionHistoryView.fxml", "Lịch sử đấu giá");
  }
}
