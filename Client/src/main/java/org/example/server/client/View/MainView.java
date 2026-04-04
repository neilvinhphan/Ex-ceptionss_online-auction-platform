package org.example.server.client.View;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.util.*;

public class MainView {
    private VBox root;
    private HeaderView header; // Dùng chung
    private Button btnSearch;
    private ComboBox<String> categoryBox;
    private TextField searchInput;
    private Map<String, VBox> categoryNodes = new HashMap<>();

    public MainView() {
        initUI();
    }

    private void initUI() {
        root = new VBox();
        StackPane heroContainer = new StackPane();
        VBox.setVgrow(heroContainer, Priority.ALWAYS);

        // Background (Giữ nguyên logic của ông)
        heroContainer.setStyle("-fx-background-color: #2c3e50;");

        VBox heroContent = new VBox(50);
        heroContent.setAlignment(Pos.TOP_CENTER);
        heroContent.setPadding(new Insets(20, 50, 20, 50));

        header = new HeaderView(); // Gọi Header dùng chung

        // Search Area (Giữ nguyên code của ông)
        VBox searchArea = new VBox(20);
        searchArea.setAlignment(Pos.CENTER);
        Label title = new Label("Tìm kiếm tài sản đấu giá");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 36));

        HBox searchContainer = new HBox(10);
        searchContainer.setAlignment(Pos.CENTER);
        searchContainer.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 35;");

        categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Bất động sản", "Phương tiện", "Đồ cổ");
        categoryBox.getSelectionModel().selectFirst();

        searchInput = new TextField();
        searchInput.setPromptText("Tìm kiếm sản phẩm...");

        btnSearch = new Button("Tìm kiếm");
        btnSearch.setStyle("-fx-background-color: #218c74; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 12 30; -fx-font-weight: bold;");

        searchContainer.getChildren().addAll(categoryBox, searchInput, btnSearch);
        searchArea.getChildren().addAll(title, searchContainer);

        heroContent.getChildren().addAll(header, searchArea);
        heroContainer.getChildren().add(heroContent);

        // Categories Section (Giữ nguyên logic FlowPane của ông)
        VBox categoriesSection = new VBox(30);
        categoriesSection.setPadding(new Insets(40));
        categoriesSection.setStyle("-fx-background-color: white;");
        // ... (Code createCategoryItem tương tự như ông đã viết)

        root.getChildren().addAll(heroContainer, categoriesSection);
    }

    public VBox getRoot() { return root; }
    public HeaderView getHeader() { return header; }
    public Button getBtnSearch() { return btnSearch; }
    public ComboBox<String> getCategoryBox() { return categoryBox; }
    public Map<String, VBox> getCategoryNodes() { return categoryNodes; }
}