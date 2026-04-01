package org.example.frontend;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.InputStream;

public class AuctionHeroUI extends Application {
    public final String GREEN_COLOR = "#218c74";
    public final String DARK_TRANSPARENT = "rgba(0, 0, 0, 0.6)";
    public final String GLASS_TRANSPARENT = "rgba(255, 255, 255, 0.2)";

    @Override
    public void start(Stage primaryStage) {
        VBox manhinh = new VBox();
        manhinh.setStyle("-fx-background-color: white;");
        StackPane heroContainer = new StackPane();
        VBox.setVgrow(heroContainer, Priority.ALWAYS);

        try {
            InputStream imageStream = getClass().getResourceAsStream("/images/anh_nen.jpg");
            if (imageStream != null) {
                Image anhnen = new Image(imageStream);
                BackgroundSize bgSize = new BackgroundSize(100, 100, true, true, false, true);
                BackgroundImage backgroundImage = new BackgroundImage(anhnen, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, bgSize);
                heroContainer.setBackground(new Background(backgroundImage));
            } else {
                heroContainer.setStyle("-fx-background-color: #2c3e50;");
            }
        } catch (Exception e) {
            heroContainer.setStyle("-fx-background-color: #2c3e50;");
        }

        VBox heroContent = new VBox(50);
        heroContent.setAlignment(Pos.TOP_CENTER);
        heroContent.setPadding(new Insets(20, 50, 20, 50));
        heroContent.getChildren().addAll(createHeader(), createHeroSection());
        heroContainer.getChildren().add(heroContent);

        VBox categoryButtons = createCategoryButtons();
        manhinh.getChildren().addAll(heroContainer, categoryButtons);

        Scene scene = new Scene(manhinh, 1200, 750);
        scene.getRoot().setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif;");

        primaryStage.setTitle("Trang chủ");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 25, 10, 25));
        header.setStyle("-fx-background-color: " + DARK_TRANSPARENT + "; -fx-background-radius: 30;");

        Label logoText = new Label("ĐẤU GIÁ");
        logoText.setTextFill(Color.web(GREEN_COLOR));
        logoText.setFont(Font.font("System", FontWeight.BOLD, 18));

        HBox navBox = new HBox(20);
        navBox.setAlignment(Pos.CENTER);
        navBox.setPadding(new Insets(0, 0, 0, 40));
        String[] links = {"Trang chủ", "Danh mục đấu giá", "Phòng đấu giá", "Giới thiệu", "Liên hệ"};

        String ACTIVE_STYLE = "-fx-background-color: rgba(255,255,255,0.2); -fx-padding: 5 15; -fx-background-radius: 15; -fx-text-fill: white; -fx-cursor: hand;";
        String DEFAULT_STYLE = "-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;";

        java.util.List<Button> menuButtons = new java.util.ArrayList<>();

        for (String text : links) {
            Button navButton = new Button(text);

            if (text.equals("Trang chủ")) {
                navButton.setStyle(ACTIVE_STYLE);
            } else {
                navButton.setStyle(DEFAULT_STYLE);
            }

            menuButtons.add(navButton);

            navButton.setOnAction(event -> {
                for (Button btn : menuButtons) {
                    btn.setStyle(DEFAULT_STYLE);
                }
                navButton.setStyle(ACTIVE_STYLE);

                switch (text) {
                    case "Trang chủ":
                        System.out.println("Đang chuyển hướng -> Màn hình Trang chủ");
                        // TODO: Gọi hàm mở trang chủ
                        break;

                    case "Danh mục đấu giá":
                        System.out.println("Đang chuyển hướng -> Màn hình Danh mục");
                        // TODO: Gọi hàm mở danh mục
                        break;

                    case "Phòng đấu giá":
                        System.out.println("Đang chuyển hướng -> Màn hình Phòng đấu giá");
                        // TODO: Gọi hàm mở phòng đấu giá
                        break;

                    case "Giới thiệu":
                        System.out.println("Đang chuyển hướng -> Màn hình Giới thiệu");
                        // TODO: Gọi hàm mở trang giới thiệu
                        break;

                    case "Liên hệ":
                        System.out.println("Đang chuyển hướng -> Màn hình Liên hệ");
                        // TODO: Gọi hàm mở trang liên hệ
                        break;
                }
            });

            navBox.getChildren().add(navButton);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox authBox = new HBox(15);
        authBox.setAlignment(Pos.CENTER);

        Button btnRegister = new Button("Đăng ký");
        btnRegister.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-weight: bold;");
        btnRegister.setOnAction(e -> {
            Stage currentStage = (Stage) btnRegister.getScene().getWindow();
            currentStage.close();
            Register register = new Register();
            Stage st_register = new Stage();
            register.start(st_register);
        });
        Button btnLogin = new Button("Đăng nhập");
        btnLogin.setStyle("-fx-background-color: " + GREEN_COLOR + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-weight: bold;");
        btnLogin.setOnAction(e -> {
            Stage currentStage = (Stage) btnLogin.getScene().getWindow();
            currentStage.close();
            Login signinUi = new Login();
            Stage st_signin = new Stage();
            signinUi.start(st_signin);
        });
        authBox.getChildren().addAll(btnRegister, btnLogin);
        header.getChildren().addAll(logoText, navBox, spacer, authBox);
        return header;
    }

    private VBox createHeroSection() {
        VBox heroBox = new VBox(20);
        heroBox.setAlignment(Pos.CENTER);
        heroBox.setPadding(new Insets(50, 0, 0, 0));

        Label title = new Label("Tìm kiếm tài sản đấu giá");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 36));

        Label subtitle = new Label("Chọn loại tài sản đấu giá và bắt đầu tìm kiếm");
        subtitle.setTextFill(Color.WHITE);
        subtitle.setFont(Font.font("System", 14));

        HBox searchContainer = new HBox(10);
        searchContainer.setAlignment(Pos.CENTER);
        searchContainer.setMaxWidth(750);
        searchContainer.setPadding(new Insets(8));
        searchContainer.setStyle("-fx-background-color: " + GLASS_TRANSPARENT + ";" + "-fx-border-color: rgba(255,255,255,0.4); -fx-border-width: 1px;" + "-fx-border-radius: 30; -fx-background-radius: 30;");

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Bất động sản", "Phương tiện","Đồ cổ", "Tác phẩm nghệ thuật ","Trang sức","Khác");
        categoryBox.getSelectionModel().selectFirst();
        categoryBox.setPrefWidth(180);
        categoryBox.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 5;");

        TextField searchInput = new TextField();
        searchInput.setPromptText("Tìm kiếm sản phẩm đấu giá");
        searchInput.setPrefWidth(400);
        searchInput.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 12 15;");

        Button btnSearch = new Button("Tìm kiếm");
        btnSearch.setStyle("-fx-background-color: " + GREEN_COLOR + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 10 30; -fx-font-weight: bold; -fx-cursor: hand;");

        searchContainer.getChildren().addAll(categoryBox, searchInput, btnSearch);
        heroBox.getChildren().addAll(title, subtitle, searchContainer);
        return heroBox;
    }

    private VBox createCategoryButtons() {
        HBox container1 = new HBox(150);
        HBox container2 = new HBox(150);
        VBox container = new VBox(0);

        container1.setAlignment(Pos.CENTER);
        container1.setPadding(new Insets(40, 0, 40, 0));
        container1.setStyle("-fx-background-color: white;");
        container2.setAlignment(Pos.CENTER);
        container2.setPadding(new Insets(40, 0, 40, 0));
        container2.setStyle("-fx-background-color: white;");

        VBox btn1 = createSingleCategoryButton("\uD83C\uDFE0","Bất động sản");
        VBox btn2 = createSingleCategoryButton("\uD83D\uDE97", "Phương tiện");
        VBox btn3 = createSingleCategoryButton("\uD83C\uDFFA", "Đồ cổ");
        VBox btn4 = createSingleCategoryButton("\uD83C\uDFA8", "Tác phẩm nghệ thuật");
        VBox btn5 = createSingleCategoryButton("\uD83D\uDC8D", "Trang sức");
        VBox btn6 = createSingleCategoryButton("…","Khác");
        container1.getChildren().addAll(btn1, btn2, btn3);
        container2.getChildren().addAll(btn4,btn5,btn6);
        container.getChildren().addAll(container1,container2);
        container.setAlignment(Pos.CENTER);
        VBox.setVgrow(container1,Priority.ALWAYS);
        VBox.setVgrow(container2,Priority.ALWAYS);
        return container;
    }

    private VBox createSingleCategoryButton(String iconStr, String text) {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-cursor: hand;");

        StackPane circle = new StackPane();
        circle.setPrefSize(90, 90);
        circle.setMaxSize(90, 90);
        circle.setStyle("-fx-background-color: #e8f9ec; -fx-background-radius: 50%;");

        Label iconLabel = new Label(iconStr);
        iconLabel.setStyle("-fx-font-size: 35px;");
        circle.getChildren().add(iconLabel);

        Label titleLabel = new Label(text);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.BLACK);

        box.getChildren().addAll(circle, titleLabel);

        box.setOnMouseEntered(e -> circle.setStyle("-fx-background-color: #d1f0db; -fx-background-radius: 50%;"));
        box.setOnMouseExited(e -> circle.setStyle("-fx-background-color: #e8f9ec; -fx-background-radius: 50%;"));

        box.setOnMouseClicked(e -> {
            // e.getButton() giúp kiểm tra xem người dùng bấm chuột trái hay phải (PRIMARY là chuột trái)
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                switch (text) {
                    case "Electronics":
                        // TODO: Gọi hàm mở trang
                        break;
                    case "Art":
                        // TODO: Gọi hàm mở trang
                        break;
                    case "Vehicle":
                        // TODO: Gọi hàm mở trang
                        break;
                }
            }
        });

        return box;
    }

    public static void main(String[] args) {
        launch(args);
    }
}