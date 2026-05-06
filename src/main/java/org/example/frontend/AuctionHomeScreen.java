package org.example.frontend;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.backend.models.Auction;
import org.example.database.AuctionDAO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionHomeScreen extends Application {

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private String currentUsername;
    private VBox auctionListBox;
    private ScheduledExecutorService scheduler;

    public AuctionHomeScreen() {
        this.currentUsername = "guest";
    }

    public AuctionHomeScreen(String username) {
        this.currentUsername = username;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hệ thống Đấu giá - Trang chủ");

        // ===== HEADER =====
        HBox header = new HBox();
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #2C3E50;");

        Text logo = new Text("🏆 HỆ THỐNG ĐẤU GIÁ");
        logo.setFont(Font.font("Tahoma", FontWeight.BOLD, 18));
        logo.setFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label("👤 " + currentUsername);
        userLabel.setStyle("-fx-text-fill: #ECF0F1; -fx-font-size: 13px;");

        Button refreshBtn = new Button("🔄 Làm mới");
        refreshBtn.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> loadAuctions());

        header.getChildren().addAll(logo, spacer, userLabel, new Label("   "), refreshBtn);

        // ===== BODY =====
        auctionListBox = new VBox(15);
        auctionListBox.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(auctionListBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #ECF0F1;");

        // ===== ROOT =====
        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 950, 650);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Tải dữ liệu lần đầu
        loadAuctions();

        // Tự động làm mới mỗi 10 giây
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> Platform.runLater(this::loadAuctions), 10, 10, TimeUnit.SECONDS);

        // Dừng scheduler khi đóng cửa sổ
        primaryStage.setOnCloseRequest(e -> {
            if (scheduler != null) scheduler.shutdownNow();
        });
    }

    /**
     * Tải danh sách phiên đấu giá từ server (database) và hiển thị lên giao diện.
     */
    private void loadAuctions() {
        auctionListBox.getChildren().clear();

        // Tiêu đề section
        Text title = new Text("📋 CÁC PHIÊN ĐẤU GIÁ ĐANG DIỄN RA");
        title.setFont(Font.font("Tahoma", FontWeight.BOLD, 16));
        title.setFill(Color.web("#2C3E50"));
        auctionListBox.getChildren().add(title);

        List<Auction> auctions = auctionDAO.getActiveAuctions();

        if (auctions.isEmpty()) {
            Label noData = new Label("Hiện chưa có phiên đấu giá nào đang diễn ra.");
            noData.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 14px;");
            auctionListBox.getChildren().add(noData);
            return;
        }

        for (Auction auction : auctions) {
            auctionListBox.getChildren().add(buildAuctionCard(auction));
        }
    }

    /**
     * Tạo card hiển thị thông tin một phiên đấu giá:
     * - Tên sản phẩm, mô tả
     * - Giá hiện tại (current_price từ server)
     * - Người dẫn đầu (leader_username từ server)
     * - Nút "Đặt giá ngay"
     */
    private HBox buildAuctionCard(Auction auction) {
        HBox card = new HBox(20);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-border-color: #BDC3C7; -fx-border-radius: 10; -fx-border-width: 1;");

        // Thông tin sản phẩm
        VBox infoBox = new VBox(6);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label itemName = new Label("🏷️  " + auction.getItemName());
        itemName.setFont(Font.font("Tahoma", FontWeight.BOLD, 14));
        itemName.setStyle("-fx-text-fill: #2C3E50;");

        Label itemDesc = new Label(auction.getItemDescription() != null ? auction.getItemDescription() : "");
        itemDesc.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 12px;");
        itemDesc.setWrapText(true);

        infoBox.getChildren().addAll(itemName, itemDesc);

        // Giá hiện tại
        VBox priceBox = new VBox(4);
        priceBox.setAlignment(Pos.CENTER);
        priceBox.setPrefWidth(160);

        Label priceLabel = new Label("💰 Giá hiện tại");
        priceLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 11px;");

        BigDecimal currentPrice = auction.getCurrentPrice() != null
                ? auction.getCurrentPrice()
                : auction.getStartingPrice();
        Label priceValue = new Label(String.format("%,.0f VNĐ", currentPrice));
        priceValue.setFont(Font.font("Tahoma", FontWeight.BOLD, 15));
        priceValue.setStyle("-fx-text-fill: #E74C3C;");

        priceBox.getChildren().addAll(priceLabel, priceValue);

        // Người dẫn đầu
        VBox leaderBox = new VBox(4);
        leaderBox.setAlignment(Pos.CENTER);
        leaderBox.setPrefWidth(140);

        Label leaderLabel = new Label("🥇 Người dẫn đầu");
        leaderLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 11px;");

        String leader = auction.getLeaderUsername() != null
                ? auction.getLeaderUsername()
                : "Chưa có";
        Label leaderValue = new Label(leader);
        leaderValue.setFont(Font.font("Tahoma", FontWeight.BOLD, 13));
        leaderValue.setStyle("-fx-text-fill: #27AE60;");

        leaderBox.getChildren().addAll(leaderLabel, leaderValue);

        // Nút đặt giá ngay
        Button bidBtn = new Button("Đặt giá ngay");
        bidBtn.setPrefWidth(130);
        bidBtn.setPrefHeight(40);
        bidBtn.setStyle("-fx-background-color: #E67E22; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 13px;");
        bidBtn.setOnAction(e -> openBidDialog(auction, currentPrice, priceValue, leaderValue));

        card.getChildren().addAll(infoBox, priceBox, leaderBox, bidBtn);
        return card;
    }

    /**
     * Mở hộp thoại nhập giá đặt.
     * Sau khi đặt thành công, cập nhật lại giá hiện tại và người dẫn đầu ngay trên card
     * mà không cần tải lại toàn bộ danh sách.
     */
    private void openBidDialog(Auction auction, BigDecimal lastPrice,
                               Label priceValueLabel, Label leaderValueLabel) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Đặt giá - " + auction.getItemName());
        dialog.setHeaderText("Giá hiện tại: " + String.format("%,.0f VNĐ", lastPrice)
                + "\nNhập giá bạn muốn đặt (phải cao hơn giá hiện tại):");

        ButtonType confirmBtn = new ButtonType("Xác nhận", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmBtn, ButtonType.CANCEL);

        TextField bidField = new TextField();
        bidField.setPromptText("Nhập giá (VNĐ)");
        bidField.setStyle("-fx-font-size: 14px;");

        VBox content = new VBox(10, new Label("Giá đặt:"), bidField);
        content.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == confirmBtn) return bidField.getText().trim();
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                // Loại bỏ dấu phẩy nếu người dùng nhập theo định dạng 1,000,000
                BigDecimal amount = new BigDecimal(input.replace(",", "").replace(".", ""));
                String error = auctionDAO.placeBid(auction.getId(), currentUsername, amount);
                if (error == null) {
                    // Thành công: làm mới thông tin card từ server
                    Auction updated = auctionDAO.refreshAuction(auction.getId());
                    if (updated != null) {
                        BigDecimal newPrice = updated.getCurrentPrice() != null
                                ? updated.getCurrentPrice()
                                : updated.getStartingPrice();
                        priceValueLabel.setText(String.format("%,.0f VNĐ", newPrice));
                        leaderValueLabel.setText(updated.getLeaderUsername() != null
                                ? updated.getLeaderUsername() : "Chưa có");
                    }
                    showAlert(Alert.AlertType.INFORMATION, "Thành công",
                            "Đặt giá " + String.format("%,.0f VNĐ", amount) + " thành công!");
                } else {
                    showAlert(Alert.AlertType.WARNING, "Không thể đặt giá", error);
                }
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng nhập một số hợp lệ.");
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
