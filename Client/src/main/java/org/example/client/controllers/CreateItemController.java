package org.example.client.controllers;

import com.google.gson.Gson;

import org.example.client.network.AuctionClient;
import org.example.client.network.ClientManager;
import org.example.client.utils.UserSession;
import org.example.core.dto.CreateArtItemDTO;
import org.example.core.dto.CreateElectronicsItemDTO;
import org.example.core.dto.CreateItemRequestDTO;
import org.example.core.dto.CreateVehicleItemDTO;
import org.example.core.dto.Request;
import org.example.core.dto.Response;
import org.example.core.models.users.User;

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

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.ResourceBundle;

public class CreateItemController extends BaseController implements Initializable {
  @FXML private MenuButton menuDanhMuc;
  @FXML private MenuButton menuUser;
  @FXML private Button createAuction;
  @FXML private TextField tfItemName;
  @FXML private ComboBox<String> cbCategory;
  @FXML private TextField tfDescription;
  @FXML private TextField tfStartingPrice;
  // --- Các VBox chứa thuộc tính riêng biệt ---
  @FXML private VBox vbArtAttributes;
  @FXML private VBox vbElectronicAttributes;
  @FXML private VBox vbVehicleAttributes;
  // Các TextField của Tác phẩm nghệ thuật
  @FXML private TextField tfArtist;
  @FXML private TextField tfCreationYear;
  // Các TextField của Đồ điện tử
  @FXML private TextField tfBrand;
  @FXML private TextField tfWarranty;
  @FXML private TextField tfCondition;
  // Các TextField của Phương tiện
  @FXML private TextField tfVehicleBrand;
  @FXML private TextField tfModel;
  @FXML private TextField tfMfgYear;
  @FXML private TextField tfMileage;
  @FXML private Button btnSubmit;
  // --- Thành phần tải ảnh ---
  @FXML private Button btnChooseImage;
  @FXML private ImageView imagePreview;
  private File selectedImageFile; // Lưu trữ file ảnh người dùng đã chọn

  private Gson gson = ClientManager.getInstance().getGson();
  private final AuctionClient clientSocket = ClientManager.getInstance().getClient();

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    // Lắng nghe sự kiện khi người dùng chọn một mục trong ComboBox
    cbCategory
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              updateDynamicFields(newValue);
            });
    User currentUser = UserSession.getInstance().getCurrentUser();
    if (currentUser != null) {
      menuUser.setText(currentUser.getUserName());
    }
  }

  /** Hàm này có nhiệm vụ ẨN/HIỆN các VBox tương ứng với danh mục được chọn */
  private void updateDynamicFields(String category) {
    // Mặc định ẩn tất cả
    hideAllDynamicFields();
    if (category == null) return;
    // Bật hiển thị VBox tương ứng
    switch (category) {
      case "ART":
        vbArtAttributes.setVisible(true);
        vbArtAttributes.setManaged(true);
        break;
      case "ELECTRONICS":
        vbElectronicAttributes.setVisible(true);
        vbElectronicAttributes.setManaged(true);
        break;
      case "VEHICLE":
        vbVehicleAttributes.setVisible(true);
        vbVehicleAttributes.setManaged(true);
        break;
      case "Khác":
        // Nếu chọn "Khác" thì chỉ nhập thông tin cơ bản, không hiện thêm gì
        break;
    }
  }

  private void hideAllDynamicFields() {
    vbArtAttributes.setVisible(false);
    vbArtAttributes.setManaged(false);

    vbElectronicAttributes.setVisible(false);
    vbElectronicAttributes.setManaged(false);

    vbVehicleAttributes.setVisible(false);
    vbVehicleAttributes.setManaged(false);
  }

  @FXML
  void handleChooseImage(ActionEvent event) {
    // Lấy cửa sổ hiện tại
    Window window = btnChooseImage.getScene().getWindow();

    // Mở hộp thoại chọn file
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Chọn hình ảnh tài sản");

    // Bộ lọc chỉ cho phép chọn ảnh PNG, JPG
    fileChooser
        .getExtensionFilters()
        .addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

    selectedImageFile = fileChooser.showOpenDialog(window);

    // --- PHẦN BỔ SUNG ĐỂ PREVIEW ---
    if (selectedImageFile != null) {
      try {
        // Chuyển file thành URI string để JavaFX Image có thể đọc được
        Image image = new Image(selectedImageFile.toURI().toString());

        // Đổ ảnh vào ImageView
        imagePreview.setImage(image);

        // (Tùy chọn) Chỉnh lại khung hình cho đẹp nếu chưa chỉnh trong FXML
        imagePreview.setPreserveRatio(true);

        System.out.println("Đã nạp ảnh preview từ: " + selectedImageFile.getName());
      } catch (Exception e) {
        e.printStackTrace();
        showAlert("Lỗi", "Không thể hiển thị ảnh xem trước!");
      }
    }
  }

  @FXML
  void handleSubmit(ActionEvent event) {

    CreateItemRequestDTO itemDTO;

    // ===== 1. LẤY DATA =====
    String name = tfItemName.getText();
    String category = cbCategory.getValue();
    String description = tfDescription.getText();
    int sellerId = UserSession.getInstance().getCurrentUser().getUserId();
    System.out.println("====== KIỂM TRA SELLER ID: " + sellerId + " ======");
    BigDecimal staringPrice;

    if (name.isEmpty() || category == null) {
      showAlert("Lỗi", "Vui lòng nhập tên và chọn phân loại!");
      return;
    }

    try {
      // ===== 2. TẠO DTO THEO CATEGORY =====
      switch (category) {
        case "ART":
          CreateArtItemDTO artDTO = new CreateArtItemDTO();
          artDTO.setArtist(tfArtist.getText());
          artDTO.setCreationYear(Integer.parseInt(tfCreationYear.getText().trim()));
          itemDTO = artDTO;
          break;

        case "ELECTRONICS":
          CreateElectronicsItemDTO elecDTO = new CreateElectronicsItemDTO();
          elecDTO.setBrand(tfBrand.getText());
          elecDTO.setWarrantyMonths(Integer.parseInt(tfWarranty.getText().trim()));
          elecDTO.setCondition(tfCondition.getText());
          itemDTO = elecDTO;
          break;

        case "VEHICLE":
          CreateVehicleItemDTO vehDTO = new CreateVehicleItemDTO();
          vehDTO.setBrand(tfVehicleBrand.getText());
          vehDTO.setModel(tfModel.getText());
          vehDTO.setManufacturingYear(Integer.parseInt(tfMfgYear.getText().trim()));
          vehDTO.setMileage(Double.parseDouble(tfMileage.getText()));
          itemDTO = vehDTO;
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
      }

      // ===== 3. SET FIELD CHUNG =====
      itemDTO.setItemName(name);
      itemDTO.setType(category);
      itemDTO.setDescription(description);
      itemDTO.setSellerID(sellerId);
      if (selectedImageFile != null) {
        String base64String = encodeFileToBase64(selectedImageFile);
        if (base64String != null) {
          itemDTO.setBase64Image(base64String);
        }
      }
      System.out.println("Tao luong");
      try {
        Request request = new Request("CREATE_ITEM", itemDTO);
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
                            System.out.println(response.getStatus());
                            System.out.println("DATA =" + response.getData());
                            showAlert(
                                "Thành công",
                                "Tạo sản phẩm đấu giá thành công! Chuyển sang trang kho hàng...");
                            switchScene(event, "/views/WareHouseView.fxml", "Kho hàng");
                          } else {
                            showAlert("Tạo sản phẩm đấu giá thất bại!", response.getMessage());
                          }
                          System.out.println("DATA =" + response.getData());
                        });
                  } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(
                        () ->
                            showAlert(
                                "Lỗi kết nối", "Không thể kết nối đến server: " + ex.getMessage()));
                  }
                })
            .start();
      } catch (Exception e) {
        showAlert("Tạo sản phẩm đấu giá thất bại!", e.getMessage());
      }

      // ===== 5. DEBUG =====
      //            System.out.println("DTO: " + itemDTO);

      // ===== 6. GỬI SERVER =====
      // sendToServer(itemDTO);

      //            showAlert("Thành công", "Đã tạo sản phẩm đấu giá!");
      //            switchScene(event, "/views/WareHouseView.fxml", "Sản phẩm chờ đấu giá");

    } catch (NumberFormatException e) {
      showAlert("Lỗi nhập liệu", "Vui lòng nhập số hợp lệ!");
    } catch (Exception e) {
      showAlert("Lỗi hệ thống", e.getMessage());
    }
  }

  @FXML
  void handleMain(ActionEvent event) {
    switchScene(event, "/views/MainView.fxml", "Trang chủ");
  }

  @FXML
  void handleMenuItem(ActionEvent event) {
    switchScene(event, "/views/AuctionCatalogView.fxml", "Danh sach phong dau gia");
  }

  @FXML
  public void handleHistoryAuction(ActionEvent event) {
  switchScene(event, "/views/AuctionHistoryView.fxml", "Lich su dau gia");
  }

  @FXML
  void handleUserUi(ActionEvent event) {
    switchScene(event, "/views/PersonalView.fxml", "Ho so ca nhan");
  }

  @FXML
  void handleLogout(ActionEvent event) {
    switchScene(event, "/views/LoginView.fxml", "Dang nhap he thong");
  }

  @FXML
  void handleCreateAuction(ActionEvent event) {
    switchScene(event, "/views/CreateAuctionView.fxml", "Tao cuoc dau gia");
  }

  @FXML
  void handleWareHouse(ActionEvent event) {
    switchScene(event, "/views/WareHouseView.fxml", "Kho hàng");
  }

  private String encodeFileToBase64(File file) { // mã hóa ảnh
    try {
      byte[] fileContent = Files.readAllBytes(file.toPath());
      return Base64.getEncoder().encodeToString(fileContent);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @FXML
  public void handleWaitPayment(ActionEvent event) {
    switchScene(event, "/views/WaitPaymentView.fxml", "San pham cho thanh toan");

  }
}
