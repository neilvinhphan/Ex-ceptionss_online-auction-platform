package org.example.client.controllers.admin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.example.client.controllers.BaseController;
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
import org.example.core.shared.enums.ActionType;
import org.example.core.shared.enums.AuctionStatus;

/**
 * Controller quản lý danh sách và các thao tác liên quan đến toàn bộ phiên đấu giá hệ thống dành
 * cho Admin.
 */
public class ManageAuctionController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(ManageAuctionController.class.getName());

  @FXML private TextField searchField;
  @FXML private TableView<Auction> auctionTable;
  @FXML private TableColumn<Auction, Integer> colId;
  @FXML private TableColumn<Auction, String> colItemName;
  @FXML private TableColumn<Auction, String> colSeller;
  @FXML private TableColumn<Auction, String> colStartTime;
  @FXML private TableColumn<Auction, BigDecimal> colCurrentPrice;
  @FXML private TableColumn<Auction, String> colStatus;

  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();
  private final ObservableList<Auction> auctionList = FXCollections.observableArrayList();

  /**
   * Khởi tạo cấu trúc các cột của bảng hiển thị và gửi yêu cầu đồng bộ dữ liệu phiên đấu giá từ
   * Server.
   */
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    setupTableColumns();
    loadAuctionsFromServer();
  }

  /**
   * Thiết lập cấu trúc ánh xạ dữ liệu động từ thực thể Auction lên các thành phần cột giao diện
   * bảng.
   */
  private void setupTableColumns() {
    colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));

    colItemName.setCellValueFactory(
        cellData -> {
          if (cellData.getValue().getItem() != null) {
            return new SimpleStringProperty(cellData.getValue().getItem().getItemName());
          }
          return new SimpleStringProperty("Sản phẩm bị lỗi");
        });

    colSeller.setCellValueFactory(new PropertyValueFactory<>("ownerId"));

    DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    colStartTime.setCellValueFactory(
        cellData -> {
          if (cellData.getValue().getStartTime() != null) {
            return new SimpleStringProperty(cellData.getValue().getStartTime().format(formatterDate));
          }
          return new SimpleStringProperty("Chưa có");
        });

    colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("highestBid"));
    DecimalFormat formatterPrice = new DecimalFormat("#,###", new DecimalFormatSymbols(new Locale("vi", "VN")));
        colCurrentPrice.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatterPrice.format(item) + " VND");
                }
            }
        });

      colStatus.setCellValueFactory(
        cellData -> {
          AuctionStatus statusEnum = cellData.getValue().getStatus();
          return new SimpleStringProperty(
              statusEnum != null ? statusEnum.name() : "Không xác định");
        });

    auctionTable.setItems(auctionList);
  }

  /**
   * Khởi chạy tiến trình chạy ngầm gửi yêu cầu tải toàn bộ danh sách phiên đấu giá hiện có trên
   * Server.
   */
  private void loadAuctionsFromServer() {
    User currentUser = UserSession.getInstance().getCurrentUser();
    if (currentUser == null) {
      showAlert("Lỗi", "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn!");
      return;
    }

    Request request = new Request(ActionType.ADMIN_GET_ALL_AUCTIONS, null);
    String jsonRequest = gson.toJson(request);

    new Thread(
            () -> {
              try {
                logger.info("Đang lấy danh sách tất cả phiên đấu giá từ Server...");
                String jsonResponse = clientSocket.sendRequest(jsonRequest);
                Response response = gson.fromJson(jsonResponse, Response.class);

                if ("SUCCESS".equals(response.getStatus())) {
                  String jsonData = gson.toJson(response.getData());
                  JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
                  List<Auction> fetchedAuctions = new ArrayList<>();

                  for (JsonElement element : jsonArray) {
                    JsonObject auctionObj = element.getAsJsonObject();
                    Auction auction = gson.fromJson(auctionObj, Auction.class);

                    if (auctionObj.has("item") && !auctionObj.get("item").isJsonNull()) {
                      JsonObject itemObj = auctionObj.getAsJsonObject("item");
                      String type = itemObj.get("type").getAsString();

                      Item parsedItem =
                          switch (type.toUpperCase()) {
                            case "ART" -> gson.fromJson(itemObj, ArtItem.class);
                            case "ELECTRONICS" -> gson.fromJson(itemObj, ElectronicsItem.class);
                            case "VEHICLE" -> gson.fromJson(itemObj, VehicleItem.class);
                            default -> null;
                          };
                      auction.setItem(parsedItem);
                    }
                    fetchedAuctions.add(auction);
                  }

                  Platform.runLater(
                      () -> {
                        auctionList.setAll(fetchedAuctions);
                        logger.info(
                            "Đã tải xong "
                                + fetchedAuctions.size()
                                + " phiên đấu giá vào bảng Quản lý.");
                      });

                } else {
                  logger.warning(
                      "Server từ chối cung cấp danh sách phiên đấu giá: " + response.getMessage());
                  Platform.runLater(() -> showAlert("Lỗi tải dữ liệu", response.getMessage()));
                }

              } catch (Exception e) {
                logger.log(
                    Level.SEVERE, "Lỗi kết nối hoặc xử lý bóc tách dữ liệu đa hình từ Server", e);
                Platform.runLater(
                    () ->
                        showAlert(
                            "Lỗi kết nối",
                            "Không thể lấy dữ liệu. Chi tiết lỗi: " + e.getMessage()));
              }
            })
        .start();
  }

  /**
   * Thực hiện tìm kiếm cục bộ và lọc dữ liệu trên bảng theo từ khóa dựa trên ID phiên hoặc tên sản
   * phẩm.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  public void handleSearch(ActionEvent event) {
    String keyword = searchField.getText().trim().toLowerCase();
    if (keyword.isEmpty()) {
      auctionTable.setItems(auctionList);
      return;
    }

    ObservableList<Auction> filteredList = FXCollections.observableArrayList();
    for (Auction auction : auctionList) {
      boolean matchId = false;
      boolean matchItemName = false;
      String idStr = String.valueOf(auction.getAuctionId());

      if (idStr.contains(keyword)) {
        matchId = true;
      }

      if (auction.getItemName() != null && auction.getItemName().toLowerCase().contains(keyword)) {
        matchItemName = true;
      } else if (auction.getItem() != null
          && auction.getItem().getItemName() != null
          && auction.getItem().getItemName().toLowerCase().contains(keyword)) {
        matchItemName = true;
      }

      if (matchId || matchItemName) {
        filteredList.add(auction);
      }
    }
    auctionTable.setItems(filteredList);
  }

  /**
   * Khôi phục thanh tìm kiếm, đặt lại bảng dữ liệu gốc và thực hiện tải mới dữ liệu từ Server.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  public void handleRefresh(ActionEvent event) {
    searchField.clear();
    if (auctionTable.getItems() != auctionList) {
      auctionTable.setItems(auctionList);
    }
    loadAuctionsFromServer();
  }

  /**
   * Thực hiện lệnh cưỡng chế hủy một phiên đấu giá đang trong trạng thái hoạt động (RUNNING) từ
   * phía Admin.
   *
   * @param event Sự kiện kích hoạt từ UI.
   */
  @FXML
  public void handleCancelAuction(ActionEvent event) {
      Auction selectedAuction = auctionTable.getSelectionModel().getSelectedItem();

      if (selectedAuction == null) {
          showAlert("Thông báo", "Vui lòng click chọn một phiên đấu giá trên bảng để hủy!");
          return;
      }

      if (selectedAuction.getStatus() != AuctionStatus.RUNNING && selectedAuction.getStatus() != null) {
          showAlert("Từ chối", "Chỉ có thể hủy những phiên đang chạy (RUNNING).");
          return;
      }

      Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
      confirm.setTitle("CẢNH BÁO KHẨN CẤP");
      confirm.setHeaderText("Hủy phiên ID: " + selectedAuction.getAuctionId());
      confirm.setContentText("Hành động này sẽ đóng phiên ngay lập tức. Không thể hoàn tác!");

      confirm.showAndWait().ifPresent(responseBtn -> {
          if (responseBtn == ButtonType.OK) {
              Request request = new Request(ActionType.ADMIN_CANCEL_AUCTION, selectedAuction.getAuctionId());

              new Thread(() -> {
                  try {
                      String jsonResponse = clientSocket.sendRequest(gson.toJson(request));
                      Response serverResponse = gson.fromJson(jsonResponse, Response.class);

                      Platform.runLater(() -> {
                          if ("SUCCESS".equals(serverResponse.getStatus())) {
                              showAlert("Thành công", "Đã hủy phiên đấu giá thành công!");
                              selectedAuction.setStatus(AuctionStatus.CANCELED);
                              auctionTable.refresh();
                          } else {
                              int code = serverResponse.getData() instanceof Number ? ((Number) serverResponse.getData()).intValue() : -1;
                              String title = (code == 4040) ? "Phòng không tồn tại" :
                                      (code == 5000) ? "Lỗi cơ sở dữ liệu" : "Từ chối thực thi";
                              showAlert(title, serverResponse.getMessage());
                          }
                      });
                  } catch (Exception e) {
                      logger.log(Level.SEVERE, "Lỗi mạng khi hủy phiên đấu giá", e);
                      Platform.runLater(() -> showAlert("Lỗi kết nối", "Chi tiết lỗi: " + e.getMessage()));
                  }
              }).start();
          }
      });
  }
}
