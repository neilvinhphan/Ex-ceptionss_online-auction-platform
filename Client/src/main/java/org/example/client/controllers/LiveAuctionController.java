package org.example.client.controllers;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.example.core.dto.BidBroadcastDTO;
import org.example.core.dto.BidRequestDTO;

import java.io.BufferedReader; // Thêm import này
import java.io.PrintWriter;
import java.math.BigDecimal;

public class LiveAuctionController {
    // =======================================================
    // 1. KHAI BÁO CÁC BIẾN GIAO DIỆN (Lấy ID từ Scene Builder)
    // =======================================================
    @FXML private TextField txtBidAmount;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblWarning;
    @FXML private Label lblLeader;

    // =======================================================
    // 2. KHAI BÁO CÁC BIẾN LƯU TRỮ VÀ ĐỒ NGHỀ SOCKET
    // =======================================================
    private int currentAuctionId = 1; // Giả sử ID phòng là 1
    private int currentUserId = 100;  // Giả sử ID người đăng nhập là 100

    private Gson gson = new Gson();
    private PrintWriter outToServer;  // Ống ghi dữ liệu gửi lên Server
    private BufferedReader inFromServer; // Ống đọc dữ liệu Server gửi về (Thêm biến này là hết đỏ!)

    // Hàm này được gọi từ bên ngoài khi mở màn hình để nạp Socket vào
    public void setSocketStreams(BufferedReader in, PrintWriter out) {
        this.inFromServer = in;
        this.outToServer = out;

        // Ngay khi có luồng mạng, bật Thread lắng nghe Server luôn
        listenFromServer();
    }

    // =======================================================
    // 3. HÀM XỬ LÝ NÚT ĐẶT GIÁ
    // =======================================================
    @FXML
    public void handleBidAction() {
        String input = txtBidAmount.getText();

        try {
            // Dùng BigDecimal cho độ chính xác tuyệt đối của tiền tệ
            BigDecimal bidAmount = new BigDecimal(input);
            BigDecimal currentPrice = new BigDecimal(lblCurrentPrice.getText());

            // 1. Validate tại Client
            if (bidAmount.compareTo(currentPrice) <= 0) {
                lblWarning.setText("Giá phải cao hơn giá hiện tại!");
                lblWarning.setVisible(true);
                return;
            }

            lblWarning.setVisible(false);
            txtBidAmount.clear(); // Xóa trắng ô nhập sau khi bấm

            // 2. Tạo DTO và gửi qua Socket
            BidRequestDTO request = new BidRequestDTO(currentAuctionId, currentUserId, bidAmount);

            // Chuyển object thành JSON chuỗi và gửi đi
            String jsonToSend = gson.toJson(request);

            // Gửi qua Socket
            if (outToServer != null) {
                outToServer.println("ACTION_BID|" + jsonToSend);
            } else {
                System.out.println("Chưa kết nối Socket tới Server!");
            }

        } catch (NumberFormatException e) {
            lblWarning.setText("Vui lòng nhập số tiền hợp lệ!");
            lblWarning.setVisible(true);
        }
    }

    // =======================================================
    // 4. LUỒNG CHẠY NGẦM LẮNG NGHE TỪ SERVER
    // =======================================================
    private void listenFromServer() {
        new Thread(() -> {
            try {
                String messageFromServer;
                // Liên tục đọc tin nhắn từ Server gửi về
                while ((messageFromServer = inFromServer.readLine()) != null) {

                    // Nếu là tin nhắn cập nhật giá
                    if (messageFromServer.startsWith("BROADCAST_BID|")) {
                        String json = messageFromServer.substring("BROADCAST_BID|".length());
                        BidBroadcastDTO data = gson.fromJson(json, BidBroadcastDTO.class);

                        // Kiểm tra xem tin nhắn có đúng là của phòng mình đang xem không
                        if (data.getAuctionId() == currentAuctionId) {

                            // Ép cập nhật lên giao diện JavaFX bằng Platform.runLater
                            Platform.runLater(() -> {
                                // NHẢY SỐ VÀ CẬP NHẬT NGƯỜI DẪN ĐẦU TRÊN GIAO DIỆN!
                                lblCurrentPrice.setText(String.valueOf(data.getNewPrice()));
                                lblLeader.setText(data.getLeaderUsername());
                            });
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Mất kết nối với Server: " + e.getMessage());
            }
        }).start();
    }
}