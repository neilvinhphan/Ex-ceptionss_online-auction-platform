package org.example.client.controllers;

import org.example.client.utils.UserSession;
import org.example.core.models.users.User;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class CreateAuctionController extends BaseController implements Initializable {
    @FXML
    private MenuButton menuUser;
    @FXML
    private Spinner<Integer> durationHourSpinner;
    @FXML
    private Spinner<Integer> durationMinuteSpinner;
    @FXML
    private DatePicker dpStartDate;
    @FXML
    private TextField tfItemName;
    @FXML
    private TextField tfStartingPrice;
    @FXML
    private ComboBox<String> cbCategory;
    @FXML
    private TextArea taDescription;
    @FXML
    private Button btnChooseImage;
    @FXML
    private ImageView imagePreview;
    private File selectedImageFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) { // hàm khởi tạo của controller (nơi setup UI sau khi FXML load xong)
        initUser();
        initSpinners();
    }

    private void initUser() {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            menuUser.setText(currentUser.getUserName());
        }
    }

    private void initSpinners() {
        durationHourSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 72, 1) // định dạng giờ: interger, min = 0, max = 72, bước nhảy 1
        );

        durationMinuteSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0)
        );

        durationHourSpinner.setEditable(true);
        durationMinuteSpinner.setEditable(true); // cho phép người dùng gõ trực tiếp vào ô
    }

    public void handleMain(ActionEvent event) {
        switchScene(event, "/views/MainView.fxml", "Trang chủ");
    }

    public void handleMenuItem(ActionEvent event) {
        MenuItem item = (MenuItem) event.getSource();
        MenuButton parent = (MenuButton) item.getParentPopup().getOwnerNode();
        parent.setText(item.getText());
        switchScene(event, "/views/AuctionCatalogView.fxml", "Danh mục sản phẩm đấu giá");
    }

    public void handleRoomAuction(ActionEvent event) {
        MenuItem item = (MenuItem) event.getSource();
        MenuButton parent = (MenuButton) item.getParentPopup().getOwnerNode();
        parent.setText(item.getText());
        System.out.println("Chuyển sang phòng đấu giá");
    }

    public void handleUserui(ActionEvent event) {
        switchScene(event, "/views/PersonalView.fxml", "Hồ sơ cá nhân");
    }

    public void handleLogout(ActionEvent event) {
        UserSession.getInstance().cleanUserSession();
        switchScene(event, "/views/LoginView.fxml", "Đăng nhập hệ thống");
    }

    public void handleCreateAuction(ActionEvent event) {
        switchScene(event, "/views/CreateAuctionView.fxml", "Tạo cuộc đấu giá");
    }

    public void handleChooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser(); //tạo ra một công cụ cho phép user chọn file từ máy
        fileChooser.setTitle("Chọn ảnh tài sản");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")//lọc loại file
        );

        Stage stage = (Stage) btnChooseImage.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage); //mở thư viện

        if (file != null) {
            selectedImageFile = file;

            Image image = new Image(file.toURI().toString()); //chuyển file thành đường dẫn dạng URL
            imagePreview.setImage(image);
        }
    }


    public CreateAuctionPayload buildPayload() { // Tạo ra một object chứa dữ liệu từ UI (UI → SERVER)
        return new CreateAuctionPayload(
                safeText(tfItemName),
                safeText(cbCategory),
                safeNumber(tfStartingPrice),
                safeText(taDescription),
                getDuration(),
                getStartDate(),
                selectedImageFile
        );
    }

    public void handleSubmit(ActionEvent event) {
        if (tfItemName.getText().trim().isEmpty() || tfStartingPrice.getText().trim().isEmpty()) {
            showAlert("Lỗi", "Vui lòng điền đầy đủ các thông tin bắt buộc!");
            return;
        }
        CreateAuctionPayload payload = buildPayload();

        System.out.println(payload);
        // gửi dữ liệu qua server ở chộ này
    }

    // =========================================================
    // 🔹 HELPERS (UI safe read)
    // =========================================================

    private String safeText(TextInputControl field) {
        return (field == null || field.getText() == null) ? "" : field.getText().trim();
    }

    private String safeText(ComboBox<String> cb) {
        return (cb == null || cb.getValue() == null) ? "" : cb.getValue().trim();
    }

    private double safeNumber(TextField field) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private int getSpinnerValue(Spinner<Integer> spinner) {
        return (spinner == null || spinner.getValue() == null) ? 0 : spinner.getValue();
    }

    private Duration getDuration() {
        return Duration.ofHours(getSpinnerValue(durationHourSpinner))
                .plusMinutes(getSpinnerValue(durationMinuteSpinner));
    }

    private LocalDate getStartDate() {
        return dpStartDate != null ? dpStartDate.getValue() : null;
    }
}


class CreateAuctionPayload { // gom dữ liệu => SERVER
    private final String itemName;
    private final String category;
    private final double startingPrice;
    private final String description;
    private final Duration duration;
    private final LocalDate startDate;
    private final File imageFile;
    public CreateAuctionPayload(String itemName,
                                String category,
                                double startingPrice,
                                String description,
                                Duration duration,
                                LocalDate startDate,
                                File imageFile) {
        this.itemName = itemName;
        this.category = category;
        this.startingPrice = startingPrice;
        this.description = description;
        this.duration = duration;
        this.startDate = startDate;
        this.imageFile = imageFile;
    }

    public String getItemName() {
        return itemName;
    }

    public String getCategory() {
        return category;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public String getDescription() {
        return description;
    }

    public Duration getDuration() {
        return duration;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public File getImageFile() {
        return imageFile;
    }
/*
    @Override
    public String toString() {
        return "CreateAuctionPayload{" +
                "itemName='" + itemName + '\'' +
                ", category='" + category + '\'' +
                ", startingPrice=" + startingPrice +
                ", duration=" + duration +
                ", startDate=" + startDate +
                ", imageFile=" + (imageFile != null ? imageFile.getName() : "null") +
                '}';
    } */
}