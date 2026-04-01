package org.example.frontend;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import java.io.InputStream;
import java.util.*;

public class MainView {
    private VBox root;
    private Button btnRegister, btnLogin, btnSearch;
    private ComboBox<String> categoryBox;
    private TextField searchInput;
    private List<Button> navButtons = new ArrayList<>();
    private Map<String, VBox> categoryNodes = new HashMap<>();

    private final String GREEN_COLOR = "#218c74";
    private final String DARK_TRANSPARENT = "rgba(0, 0, 0, 0.6)";
    private final String GLASS_TRANSPARENT = "rgba(255, 255, 255, 0.2)";

    public MainView() {
        initUI();
    }

    private void initUI() {
        root = new VBox();
        root.setFillWidth(true);

        StackPane heroContainer = new StackPane();
        VBox.setVgrow(heroContainer, Priority.ALWAYS);

        try {
            InputStream stream = getClass().getResourceAsStream("/images/anh_nen.jpg");
            if (stream != null) {
                BackgroundImage bgi = new BackgroundImage(new Image(stream),
                        BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER, new BackgroundSize(100, 100, true, true, false, true));
                heroContainer.setBackground(new Background(bgi));
            }
        } catch (Exception e) {
            heroContainer.setStyle("-fx-background-color: #2c3e50;");
        }

        VBox heroContent = new VBox(50);
        heroContent.setAlignment(Pos.TOP_CENTER);
        heroContent.setPadding(new Insets(20, 50, 20, 50));

        // --- HEADER ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 25, 10, 25));
        header.setStyle("-fx-background-color: " + DARK_TRANSPARENT + "; -fx-background-radius: 30;");

        Label logo = new Label("ĐẤU GIÁ");
        logo.setTextFill(Color.web(GREEN_COLOR));
        logo.setFont(Font.font("System", FontWeight.BOLD, 18));

        HBox navBox = new HBox(20);
        navBox.setAlignment(Pos.CENTER);
        navBox.setPadding(new Insets(0, 0, 0, 40));
        String[] links = {"Trang chủ", "Danh mục đấu giá", "Phòng đấu giá", "Giới thiệu", "Liên hệ"};
        for (String text : links) {
            Button b = new Button(text);
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
            navButtons.add(b);
            navBox.getChildren().add(b);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnRegister = new Button("Đăng ký");
        btnRegister.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-weight: bold;");

        btnLogin = new Button("Đăng nhập");
        btnLogin.setStyle("-fx-background-color: " + GREEN_COLOR + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-weight: bold;");

        header.getChildren().addAll(logo, navBox, spacer, new HBox(15, btnRegister, btnLogin));

        // --- SEARCH BAR (THANH KÍNH MỜ) ---
        VBox searchArea = new VBox(20);
        searchArea.setAlignment(Pos.CENTER);
        searchArea.setPadding(new Insets(50, 0, 0, 0));

        Label title = new Label("Tìm kiếm tài sản đấu giá");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 36));

        Label subTitle = new Label("Chọn loại tài sản và bắt đầu tìm kiếm");
        subTitle.setTextFill(Color.WHITE);

        HBox searchContainer = new HBox(10);
        searchContainer.setAlignment(Pos.CENTER);
        searchContainer.setMaxWidth(800);
        searchContainer.setPadding(new Insets(10));
        searchContainer.setStyle("-fx-background-color: " + GLASS_TRANSPARENT + "; -fx-border-color: rgba(255,255,255,0.4); -fx-border-radius: 35; -fx-background-radius: 35;");

        categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Bất động sản", "Phương tiện", "Đồ cổ", "Nghệ thuật", "Trang sức", "Khác");
        categoryBox.getSelectionModel().selectFirst();
        categoryBox.setPrefWidth(180);
        categoryBox.setStyle("-fx-background-color: white; -fx-background-radius: 25; -fx-padding: 5 10;");

        searchInput = new TextField();
        searchInput.setPromptText("Tìm kiếm sản phẩm...");
        searchInput.setPrefWidth(400);
        searchInput.setStyle("-fx-background-color: white; -fx-background-radius: 25; -fx-padding: 12 15;");

        btnSearch = new Button("Tìm kiếm");
        btnSearch.setStyle("-fx-background-color: " + GREEN_COLOR + "; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 12 30; -fx-font-weight: bold; -fx-cursor: hand;");

        searchContainer.getChildren().addAll(categoryBox, searchInput, btnSearch);
        searchArea.getChildren().addAll(title, subTitle, searchContainer);

        heroContent.getChildren().addAll(header, searchArea);
        heroContainer.getChildren().add(heroContent);

        // --- 2. CATEGORIES SECTION (DANH MỤC DƯỚI) ---
        VBox categoriesSection = new VBox(30);
        categoriesSection.setPadding(new Insets(40));
        categoriesSection.setAlignment(Pos.CENTER);
        categoriesSection.setStyle("-fx-background-color: white;");

        FlowPane flowPane = new FlowPane(80, 40); // Khoảng cách ngang 80, dọc 40
        flowPane.setAlignment(Pos.CENTER);

        String[][] cats = {
                {"\uD83C\uDFE0", "Bất động sản"}, {"\uD83D\uDE97", "Phương tiện"},
                {"\uD83C\uDFFA", "Đồ cổ"}, {"\uD83C\uDFA8", "Nghệ thuật"},
                {"\uD83D\uDC8D", "Trang sức"}, {"\u2026", "Khác"}
        };

        for (String[] cat : cats) {
            VBox catBtn = createCategoryItem(cat[0], cat[1]);
            categoryNodes.put(cat[1], catBtn);
            flowPane.getChildren().add(catBtn);
        }

        categoriesSection.getChildren().add(flowPane);
        root.getChildren().addAll(heroContainer, categoriesSection);
    }

    private VBox createCategoryItem(String icon, String title) {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-cursor: hand;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 35px;");

        StackPane circle = new StackPane(iconLabel);
        circle.setPrefSize(90, 90);
        circle.setMaxSize(90, 90);
        circle.setStyle("-fx-background-color: #e8f9ec; -fx-background-radius: 100;");

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        box.getChildren().addAll(circle, titleLabel);
        return box;
    }

    public VBox getRoot() {
        return root;
    }

    public Button getBtnLogin() {
        return btnLogin;
    }

    public Button getBtnRegister() {
        return btnRegister;
    }

    public Button getBtnSearch() {
        return btnSearch;
    }

    public ComboBox<String> getCategoryBox() {
        return categoryBox;
    }

    public TextField getSearchInput() {
        return searchInput;
    }

    public Map<String, VBox> getCategoryNodes() {
        return categoryNodes;
    }
}