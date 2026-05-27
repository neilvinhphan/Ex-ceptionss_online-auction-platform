package org.example.client.controllers.item;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.example.client.controllers.BaseController;
import org.example.client.controllers.user.UserSidebarController;
import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.dto.itemsDTO.CreateArtItemDTO;
import org.example.core.dto.itemsDTO.CreateElectronicsItemDTO;
import org.example.core.dto.itemsDTO.CreateItemRequestDTO;
import org.example.core.dto.itemsDTO.CreateVehicleItemDTO;
import org.example.core.shared.enums.ActionType;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller chịu trách nhiệm vận hành màn hình Tạo hồ sơ sản phẩm/tài sản mới (Create Item). Quản
 * lý bật/tắt động các khu vực thuộc tính đặc thù (Nghệ thuật, Điện tử, Xe cộ) và mã hóa Base64 tệp
 * ảnh.
 */
public class CreateItemController extends BaseController implements Initializable {

  private static final Logger logger = Logger.getLogger(CreateItemController.class.getName());

  @FXML private Button createAuction;
  @FXML private TextField tfItemName;
  @FXML private ComboBox<String> cbCategory;
  @FXML private TextField tfDescription;
  @FXML private TextField tfStartingPrice;
  @FXML private VBox vbArtAttributes;
  @FXML private VBox vbElectronicAttributes;
  @FXML private VBox vbVehicleAttributes;
  @FXML private TextField tfArtist;
  @FXML private TextField tfCreationYear;
  @FXML private TextField tfBrand;
  @FXML private TextField tfWarranty;
  @FXML private TextField tfCondition;
  @FXML private TextField tfVehicleBrand;
  @FXML private TextField tfModel;
  @FXML private TextField tfMfgYear;
  @FXML private TextField tfMileage;
  @FXML private Button btnSubmit;
  @FXML private Button btnChooseImage;
  @FXML private ImageView imagePreview;

  private File selectedImageFile;
  private final Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  /**
   * Thiết lập bộ lắng nghe cho ComboBox phân loại mục tài sản để cập nhật form nhập động tương ứng.
   */
  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    cbCategory
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              updateDynamicFields(newValue);
            });
  }

  /**
   * Mở hộp thoại hệ thống FileChooser cho phép người dùng nạp tệp tin hình ảnh sản phẩm và kết xuất
   * preview lên ImageView.
   */
  @FXML
  void handleChooseImage(ActionEvent event) {
    Window window = btnChooseImage.getScene().getWindow();

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Chọn hình ảnh tài sản");
    fileChooser
        .getExtensionFilters()
        .addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

    File chosenFile = fileChooser.showOpenDialog(window);

    if (chosenFile != null) {
      selectedImageFile = chosenFile;
      try {
        Image image = new Image(selectedImageFile.toURI().toString());
        imagePreview.setImage(image);
        imagePreview.setPreserveRatio(true);

        logger.log(
            Level.INFO,
            "Đã tải hình ảnh xem trước thành công từ tệp: {0}",
            selectedImageFile.getName());
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Không thể chuyển đổi uri và tải ảnh preview sản phẩm", e);
        showAlert("Lỗi", "Không thể hiển thị ảnh xem trước!");
        selectedImageFile = null;
      }
    }
    else {
      logger.log(Level.INFO, "Người dùng đã hủy chọn ảnh");
    }
  }

  /**
   * Thu thập thông tin từ giao diện, đóng gói đa hình DTO tương ứng với Category và truyền dữ liệu
   * lên máy chủ xử lý lưu trữ.
   */
  @FXML
  void handleSubmit(ActionEvent event) {
    CreateItemRequestDTO itemDTO;
    String name = tfItemName.getText().trim();
    String category = cbCategory.getValue();
    String description = tfDescription.getText().trim();
    int sellerId = UserSession.getInstance().getCurrentUser().getUserId();

    if (name.isEmpty() || category == null) {
      showAlert("Lỗi", "Vui lòng nhập tên và chọn phân loại!");
      return;
    }


    // 2. Gộp mô tả
    StringBuilder finalDescription = new StringBuilder(description);
    finalDescription.append("\n\n--- THÔNG TIN CHI TIẾT ---");
    try {
      // ===== TẠO DTO THEO CATEGORY VÀ GỘP CHUỖI MÔ TẢ =====
      switch (category) {
        case "ART":
          CreateArtItemDTO artDTO = new CreateArtItemDTO();
          artDTO.setArtist(tfArtist.getText());
          artDTO.setCreationYear(Integer.parseInt(tfCreationYear.getText().trim()));
          itemDTO = artDTO;
          // Nối chuỗi hiển thị
          finalDescription.append("\n🎨 Nghệ sĩ: ").append(tfArtist.getText());
          finalDescription.append("\n📅 Năm sáng tác: ").append(tfCreationYear.getText().trim());
          break;

        case "ELECTRONICS":
          CreateElectronicsItemDTO elecDTO = new CreateElectronicsItemDTO();
          elecDTO.setBrand(tfBrand.getText());
          elecDTO.setWarrantyMonths(Integer.parseInt(tfWarranty.getText().trim()));
          elecDTO.setCondition(tfCondition.getText());
          itemDTO = elecDTO;
          finalDescription.append("\n⚙ Thương hiệu: ").append(tfBrand.getText());
          finalDescription.append("\n🛡 Tháng bảo hành: ").append(tfWarranty.getText().trim());
          finalDescription.append("\n📌 Tình trạng: ").append(tfCondition.getText());
          break;

        case "VEHICLE":
          CreateVehicleItemDTO vehDTO = new CreateVehicleItemDTO();
          vehDTO.setBrand(tfVehicleBrand.getText());
          vehDTO.setModel(tfModel.getText());
          vehDTO.setManufacturingYear(Integer.parseInt(tfMfgYear.getText().trim()));
          vehDTO.setMileage(Double.parseDouble(tfMileage.getText()));
          itemDTO = vehDTO;
          finalDescription.append("\n🚗 Hãng xe: ").append(tfVehicleBrand.getText());
          finalDescription.append("\n🚙 Dòng xe: ").append(tfModel.getText());
          finalDescription.append("\n📅 Năm sản xuất: ").append(tfMfgYear.getText().trim());
          finalDescription.append("\n🛣 Số KM đã đi: ").append(tfMileage.getText().trim());
          break;

        default:
          showAlert("Lỗi", "Danh mục không hợp lệ!");
          return;
      }

      try {
        BigDecimal startingPrice = new BigDecimal(tfStartingPrice.getText().trim());
        itemDTO.setStartingPrice(startingPrice);
      } catch (Exception e) {
        showAlert("Lỗi", "Giá khởi điểm không hợp lệ");
        return; // Dừng lại nếu lỗi giá tiền
      }

      // ===== 3. SET FIELD CHUNG =====
      itemDTO.setItemName(name);
      itemDTO.setType(category);
      // TRUYỀN CHUỖI ĐÃ GỘP XUỐNG DTO THAY VÌ CHUỖI NGẮN GỐC
      itemDTO.setDescription(finalDescription.toString());

      itemDTO.setSellerID(sellerId);

      if (selectedImageFile != null) {
        String base64String = encodeFileToBase64(selectedImageFile);
        if (base64String != null) itemDTO.setBase64Image(base64String);
      }

      Request request = new Request(ActionType.CREATE_ITEM, itemDTO);
      String jsonRequest = gson.toJson(request);

      new Thread(() -> {
        try {
          String jsonResponse = clientSocket.sendRequest(jsonRequest);
          Response response = gson.fromJson(jsonResponse, Response.class);

          Platform.runLater(() -> {
            if (response.getStatus().equals("SUCCESS")) {
              showAlert("Thành công", "Tạo sản phẩm thành công! Chuyển sang trang kho hàng...");
              UserSidebarController.currentView = "WareHouseView.fxml";
              switchScene(event, "/views/WareHouseView.fxml", "Kho hàng");
            } else {
              int code = response.getData() instanceof Number ? ((Number) response.getData()).intValue() : 0;
              String title = switch (code) {
                case 4000 -> "Dữ liệu không hợp lệ (400)";
                case 5000 -> "Lỗi cơ sở dữ liệu (500)";
                default -> "Tạo sản phẩm thất bại (" + code + ")";
              };
              showAlert(title, response.getMessage());
            }
          });
        } catch (Exception ex) {
          Platform.runLater(() -> showAlert("Lỗi kết nối", "Không thể kết nối đến server: " + ex.getMessage()));
        }
      }).start();

    } catch (NumberFormatException e) {
      showAlert("Lỗi nhập liệu", "Vui lòng kiểm tra lại định dạng số!");
    } catch (Exception e) {
      showAlert("Lỗi hệ thống", e.getMessage());
    }
  }

  /** Ẩn/Hiện các vùng VBox thuộc tính mở rộng tương ứng với phân loại sản phẩm. */
  private void updateDynamicFields(String category) {
    hideAllDynamicFields();
    if (category == null) return;
    switch (category) {
      case "ART" -> {
        vbArtAttributes.setVisible(true);
        vbArtAttributes.setManaged(true);
      }
      case "ELECTRONICS" -> {
        vbElectronicAttributes.setVisible(true);
        vbElectronicAttributes.setManaged(true);
      }
      case "VEHICLE" -> {
        vbVehicleAttributes.setVisible(true);
        vbVehicleAttributes.setManaged(true);
      }
    }
  }

  /** Ẩn toàn bộ các trường nhập liệu động và rút không gian trên Layout Panel. */
  private void hideAllDynamicFields() {
    vbArtAttributes.setVisible(false);
    vbArtAttributes.setManaged(false);
    vbElectronicAttributes.setVisible(false);
    vbElectronicAttributes.setManaged(false);
    vbVehicleAttributes.setVisible(false);
    vbVehicleAttributes.setManaged(false);
  }

  /**
   * Thực hiện đọc mảng byte dữ liệu nhị phân của tệp tin ảnh và chuyển hóa sang chuỗi mã hóa Base64
   * String.
   */
  private String encodeFileToBase64(File file) {
    try {
      byte[] fileContent = Files.readAllBytes(file.toPath());
      return Base64.getEncoder().encodeToString(fileContent);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Gặp lỗi khi thực hiện mã hóa tệp tin sang chuỗi văn bản Base64", e);
      return null;
    }
  }
}
