package org.example.client.controllers;

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
import java.net.URL;
import java.util.ResourceBundle;

public class CreateItemController extends BaseController implements Initializable {
    @FXML private MenuButton menuDanhMuc;
    @FXML private MenuButton menuUser;
    @FXML private Button createAuction;
    @FXML private TextField tfItemName;
    @FXML private ComboBox<String> cbCategory;
    @FXML private TextField tfDescription;
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

    // =========================================================
    // XỬ LÝ CÁC NÚT BẤM (ACTIONS)
    // =========================================================

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
        // 1. Lấy dữ liệu cơ bản
        String name = tfItemName.getText();
        String category = cbCategory.getValue();
        String description = tfDescription.getText();
        // chưa biết gửi ảnh kiểu gì, phải mã hóa
        if (name.isEmpty() || category == null) {
            showAlert("Lỗi", "Vui lòng nhập tên và chọn phân loại sản phẩm!");
            return;
        }
        if(selectedImageFile != null) {
            System.out.println("Đường dẫn ảnh: " + selectedImageFile.getAbsolutePath());
        } else {
            System.out.println("Chưa có ảnh nào được chọn!");
        }
        CreateItemRequestDTO itemDTO = null;
        if (itemDTO != null) {
            itemDTO.setItemName(name);
            itemDTO.setType(category);
            itemDTO.setDescription(description);
        }
        System.out.println(itemDTO);
        // 2. Lấy dữ liệu nâng cao dựa theo danh mục
        try{
        if ("Tác phẩm nghệ thuật".equals(category)) {
            String artist = tfArtist.getText();
            int createrYear = Integer.parseInt(tfCreationYear.getText().trim());
            CreateArtItemDTO artDTO = new CreateArtItemDTO();
            artDTO.setArtist(artist);
            artDTO.setCreationYear(createrYear);
            itemDTO = artDTO;
        }
        else if ("Đồ điện tử".equals(category)) {
            String brand = tfBrand.getText();
            int warranty = Integer.parseInt(tfWarranty.getText().trim());
            String condition = tfCondition.getText();
            CreateElectronicsItemDTO elecDTO = new CreateElectronicsItemDTO();
            elecDTO.setBrand(brand);
            elecDTO.setWarrantyMonths(warranty);
            elecDTO.setCondition(condition);
            itemDTO = elecDTO;
        }
        else if ("Phương tiện".equals(category)) {
            String brand = tfBrand.getText();
            String model = tfModel.getText();
            int manufacturingYear = Integer.parseInt(tfMfgYear.getText().trim());
            double mileage = Double.parseDouble(tfMileage.getText());
            CreateVehicleItemDTO vehDTO = new CreateVehicleItemDTO();
            vehDTO.setBrand(brand);
            vehDTO.setModel(model);
            vehDTO.setManufacturingYear(manufacturingYear);
            vehDTO.setMileage(mileage);
            itemDTO = vehDTO;
        }

        // TODO: Đóng gói dữ liệu thành DTO và gửi qua Socket (Server) ở đây!

        showAlert("Thành công","Đã tạo sản phẩm đấu giá!");}
        catch (NumberFormatException e){
            showAlert("Lỗi nhập liệu", "vui ");
        }
        catch (Exception e) {
        showAlert("Lỗi hệ thống", "Có lỗi xảy ra: " + e.getMessage());
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