package org.example.client.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.CreateAuctionDTO;
import org.example.core.dto.PendingRequestDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.entities.Auction;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item; // Đảm bảo bạn đã import đúng class Item của bạn
import org.example.core.models.items.VehicleItem;
import org.example.core.models.users.User;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
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

public class CreateAuctionController extends BaseController implements Initializable {
  @FXML private MenuButton menuUser;
  @FXML private Spinner<Integer> durationHourSpinner;
  @FXML private Spinner<Integer> durationMinuteSpinner;
  @FXML private DatePicker dpStartDate;
  @FXML private ComboBox<Item> cbPendingItems;
  @FXML private ComboBox<String> cbCategory;
  @FXML private TextField tfStartingPrice;
  @FXML private TextField tfBidIncrement;
  // Danh sách lưu toàn bộ item chưa đấu giá kéo từ server về
  private List<Item> allPendingItems = new ArrayList<>();
  // Cờ chống lặp vô hạn giữa 2 cái Listener
  private boolean isAutoSelecting = false;
  private Gson gson = ClientManager.getInstance().getGson();
  private AuctionClient clientSocket = ClientManager.getInstance().getClient();

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    initUser();
    initSpinners();
    setupItemDisplayFormat(); // Định dạng cách hiển thị tên Item trong ComboBox
    loadPendingItems(); // 1. Lấy dữ liệu giả lập (hoặc từ Server)
    setupListeners(); // 2. Bật "Tai nghe" lắng nghe sự kiện Lọc / Tự động điền
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
    // TODO: GỌI SOCKET / API ĐỂ LẤY DANH SÁCH TÀI SẢN TRẠNG THÁI RUNNING/PENDING CỦA USER NÀY.
    int sellerId = UserSession.getInstance().getCurrentUser().getUserId();

    ArrayList<Item> allItem = new ArrayList<>();
    cbPendingItems.setItems(FXCollections.observableArrayList(allPendingItems));

    PendingRequestDTO requestPayload = new PendingRequestDTO(sellerId);

    requestPayload.setSellerId(sellerId);
    System.out.println("Tao luong");
    try {
      Request request = new Request("GET_PENDING_ITEMS", requestPayload);
      String jsonRequest = gson.toJson(request);
      new Thread(
              () -> {
                try {
                  System.out.println("Gui request");
                  String jsonResponse = clientSocket.sendRequest(jsonRequest);
                  Response response = gson.fromJson(jsonResponse, Response.class);
                  System.out.println("Nhan response");
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
                              default -> System.out.println("Unknown item type: " + type);
                            }

                            if (parsedItem != null) {

                              // 2. MÀNG LỌC: Chỉ những món đồ mang trạng thái DRAFT mới được cho
                              // vào list
                              if (parsedItem.getStatus()
                                  == org.example.core.shared.enums.ItemStatus.DRAFT) {
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
                              "Lỗi kết nối", "Không thể kết nối đến server: " + e.getMessage()));
                  e.printStackTrace();
                }
              })
          .start();
    } catch (Exception e) {
      showAlert("Lỗi", "Có lỗi xảy ra khi gửi yêu cầu: " + e.getMessage());
      e.printStackTrace();
    }
  }

  // Hàm này giúp ComboBox thay vì hiển thị địa chỉ bộ nhớ (org.example.Item@123)
  // thì sẽ in ra cái Tên của sản phẩm.
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
            return null; // Không cần implement vì combobox không cho phép gõ tay tạo mới
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
              // Nếu đang trong quá trình Auto-fill thì bỏ qua để tránh lỗi vòng lặp
              if (isAutoSelecting || newValue == null) return;

              ObservableList<Item> filteredList;
              if (newValue.equals("Tất cả") || newValue.equals("-- Tất cả danh mục --")) {
                filteredList = FXCollections.observableArrayList(allPendingItems);
              } else {
                // Lọc ra các item có type khớp với danh mục được chọn
                List<Item> filtered =
                    allPendingItems.stream()
                        .filter(item -> newValue.equals(item.getType()))
                        .collect(Collectors.toList());
                filteredList = FXCollections.observableArrayList(filtered);
              }
              cbPendingItems.setItems(filteredList);
              // Xóa trắng ô chọn item và giá khi người dùng vừa đổi bộ lọc
              cbPendingItems.getSelectionModel().clearSelection();
              tfStartingPrice.clear();
            });

    // 🎧 LISTENER 2: KHI NGƯỜI DÙNG CHỌN 1 TÀI SẢN (AUTO-FILL DANH MỤC VÀ GIÁ)
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
              // Bật cờ để thằng Listener 1 không bị giật mình chạy lại
              isAutoSelecting = true;
              cbCategory.setValue(selectedItem.getType());
              if (selectedItem.getStartingPrice() != null) {
                tfStartingPrice.setText(
                    selectedItem.getStartingPrice().toString()); // Dùng toString cho BigDecimal
              }
              isAutoSelecting = false;
            });
  }

  // =========================================================
  // 🔹 SUBMIT & ĐIỀU HƯỚNG
  // =========================================================

  public void handleSubmit(ActionEvent event) {
    // 1. Lấy thẳng Item đang được chọn trên ComboBox
    Item selectedItem = cbPendingItems.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      showAlert("Lỗi", "Vui lòng chọn một tài sản để tạo đấu giá!");
      return;
    }
      String bidIncrText = tfBidIncrement.getText().trim();
      if (bidIncrText.isEmpty()) {
          showAlert("Lỗi", "Vui lòng nhập bước giá!");
          return; // Dừng lại không chạy tiếp
      }
    if (getDuration().toMinutes() <= 0) {
      showAlert("Lỗi", "Thời gian đấu giá phải lớn hơn 0!");
      return;
    }
    try {
      // 3. Quy đổi thời gian
      long durationMinutes = getDuration().toMinutes();
      BigDecimal bidIncrement = new BigDecimal(tfBidIncrement.getText().trim());

      // 4. ĐÓNG GÓI VÀO DTO CHÍNH THỨC
      CreateAuctionDTO requestDTO =
          new CreateAuctionDTO(selectedItem, durationMinutes, bidIncrement);
      // 5. Gửi lên Server
      Request request =
          new Request(
              "CREATE_AUCTION",
              requestDTO); // Đổi tên lệnh "CREATE_AUCTION" cho khớp với Server của đệ nhé
      String jsonRequest = gson.toJson(request);

      new Thread(
              () -> {
                try {
                  // Bắn lên Server và chờ phản hồi
                  String jsonResponse = clientSocket.sendRequest(jsonRequest);
                  Response response = gson.fromJson(jsonResponse, Response.class);

                  // Xử lý giao diện phải dùng Platform.runLater
                  Platform.runLater(
                      () -> {
                        if ("SUCCESS".equals(response.getStatus())) {

                          // 1. MÓC DỮ LIỆU AUCTION TỪ SERVER TRẢ VỀ (Tùy cấu trúc DTO của bro)
                          String jsonData = gson.toJson(response.getData());
                          Auction newAuction = gson.fromJson(jsonData, Auction.class);

                          // 2. BƯỚC QUAN TRỌNG NHẤT: Bơm dữ liệu vào Trạm trung chuyển (Session)
                          org.example.client.utils.AuctionSession.getInstance()
                              .setCurrentAuction(newAuction);
                          org.example.client.utils.AuctionSession.getInstance()
                              .setCurrentItem(
                                  selectedItem); // selectedItem là cái đã getComboBox ở đầu hàm

                          // 3. Giờ Session đã input, tiến hành Chuyển cảnh!
                          Platform.runLater(
                              () -> {
                                showAlert("Thành công", "Đã tạo cuộc đấu giá thành công!");
                                  // Trong handleSubmit của CreateAuctionController
                                  System.out.println("DEBUG Ảnh trước khi vào phòng: " + selectedItem.getImage());
// Nếu nó in ra null hoặc "" thì lỗi là do lúc loadPendingItems chưa lấy ảnh về.
                                switchScene(event, "/views/AuctionRoomView.fxml", "Phòng đấu giá");
                              });

                        } else {
                          Platform.runLater(
                              () -> showAlert("Lỗi tạo đấu giá", response.getMessage()));
                        }
                      });
                } catch (Exception e) {
                  Platform.runLater(
                      () -> showAlert("Lỗi kết nối", "Không thể gửi yêu cầu: " + e.getMessage()));
                  e.printStackTrace();
                }
              })
          .start();

      System.out.println("Đã đóng gói DTO thành công! Tài sản: " + selectedItem.getItemName());

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
      switchScene(event, "/views/WaitPaymentView.fxml", "San pham cho thanh toan");

  }
  @FXML
  void handleCreateItem(ActionEvent event) {
    switchScene(event, "/views/CreateItemView.fxml", "Tạo sản phẩm đấu giá");
  }

  public void handleMenuItem(ActionEvent event) {
    switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục đấu giá");
  }

  // =========================================================
  // 🔹 HELPERS
  // =========================================================
  private Duration getDuration() {
    int hours = (durationHourSpinner.getValue() != null) ? durationHourSpinner.getValue() : 0;
    int minutes = (durationMinuteSpinner.getValue() != null) ? durationMinuteSpinner.getValue() : 0;
    return Duration.ofHours(hours).plusMinutes(minutes);
  }


    public void handleHistoryAuction(ActionEvent event) {
        switchScene(event, "/views/AuctionHistoryView.fxml", "Lich su dau gia");
    }
}
