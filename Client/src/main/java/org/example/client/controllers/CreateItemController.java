package org.example.client.controllers;

import org.example.client.utils.UserSession;
import org.example.core.dto.CreateArtItemDTO;
import org.example.core.dto.CreateElectronicsItemDTO;
import org.example.core.dto.CreateItemRequestDTO;
import org.example.core.dto.CreateVehicleItemDTO;

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
    // --- Thành phần tải ảnh ---
    @FXML private Button btnChooseImage;
    @FXML private ImageView imagePreview;
    private File selectedImageFile; // Lưu trữ file ảnh người dùng đã chọn

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Lắng nghe sự kiện khi người dùng chọn một mục trong ComboBox
        cbCategory.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateDynamicFields(newValue);
        });
    }
    /**
     * Hàm này có nhiệm vụ ẨN/HIỆN các VBox tương ứng với danh mục được chọn
     */
    private void updateDynamicFields(String category) {
        // Mặc định ẩn tất cả
        hideAllDynamicFields();
        if (category == null) return;
        // Bật hiển thị VBox tương ứng
        switch (category) {
            case "Tác phẩm nghệ thuật":
                vbArtAttributes.setVisible(true);
                vbArtAttributes.setManaged(true);
                break;
            case "Đồ điện tử":
                vbElectronicAttributes.setVisible(true);
                vbElectronicAttributes.setManaged(true);
                break;
            case "Phương tiện":
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
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        selectedImageFile = fileChooser.showOpenDialog(window);

        if (selectedImageFile != null) {
            // Hiển thị ảnh lên ImageView
            Image image = new Image(selectedImageFile.toURI().toString());
            imagePreview.setImage(image);
        }
    }

    @FXML
    void handleSubmit(ActionEvent event) {

        // ===== 1. LẤY DATA =====
        String name = tfItemName.getText();
        String category = cbCategory.getValue();
        String description = tfDescription.getText();

        int sellerId = UserSession.getInstance().getSellerID();
        if (sellerId == null) {
            showAlert("Lỗi", "Vui lòng đăng nhập lại!");
            return;
        }
        itemDTO.setSellerId(sellerId);

        BigDecimal staringPrice = null;
        try{
            BigDecimal startingPrice = new BigDecimal(tfStartingPrice.getText().trim());
        }
        catch (Exception e){
            showAlert("Lỗi", "Gia khoi diem khong hop le");
        }
        if (name.isEmpty() || category == null) {
            showAlert("Lỗi", "Vui lòng nhập tên và chọn phân loại!");
            return;
        }

        CreateItemRequestDTO itemDTO = null;

        try {
            // ===== 2. TẠO DTO THEO CATEGORY =====
            switch (category) {

                case "Tác phẩm nghệ thuật":
                    CreateArtItemDTO artDTO = new CreateArtItemDTO();
                    artDTO.setArtist(tfArtist.getText());
                    artDTO.setCreationYear(Integer.parseInt(tfCreationYear.getText().trim()));
                    itemDTO = artDTO;
                    break;

                case "Đồ điện tử":
                    CreateElectronicsItemDTO elecDTO = new CreateElectronicsItemDTO();
                    elecDTO.setBrand(tfBrand.getText());
                    elecDTO.setWarrantyMonths(Integer.parseInt(tfWarranty.getText().trim()));
                    elecDTO.setCondition(tfCondition.getText());
                    itemDTO = elecDTO;
                    break;

                case "Phương tiện":
                    CreateVehicleItemDTO vehDTO = new CreateVehicleItemDTO();
                    vehDTO.setBrand(tfVehicleBrand.getText());
                    vehDTO.setModel(tfModel.getText());
                    vehDTO.setManufacturingYear(Integer.parseInt(tfMfgYear.getText().trim()));
                    vehDTO.setMileage(Double.parseDouble(tfMileage.getText()));
                    itemDTO = vehDTO;
                    break;
            }

            // ===== 3. SET FIELD CHUNG =====
            itemDTO.setItemName(name);
            itemDTO.setType(category);
            itemDTO.setDescription(description);

            // ===== 4. ẢNH =====
         //   if (selectedImageFile != null) {
           //     itemDTO.setImageFile(selectedImageFile); // nếu DTO có field này
           // }

            // ===== 5. DEBUG =====
            System.out.println("DTO: " + itemDTO);

            // ===== 6. GỬI SERVER =====
            // sendToServer(itemDTO);

            showAlert("Thành công", "Đã tạo sản phẩm đấu giá!");
            switchScene(event, "/views/PendingAuctionView.fxml", "Sản phẩm chờ đấu giá");


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
        MenuItem menuItem = (MenuItem) event.getSource();
        System.out.println("Đã chọn danh mục: " + menuItem.getText());
    }

    @FXML
    void handleUserui(ActionEvent event) {
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
}