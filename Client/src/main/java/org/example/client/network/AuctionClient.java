package org.example.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Lớp thiết lập kết nối Socket trực tiếp (Low-level Socket Connection) phía Client. Đảm nhiệm việc
 * truyền nhận luồng dữ liệu thô dạng văn bản và dọn rác ống truyền mạng.
 */
public class AuctionClient {

  private static final Logger logger = Logger.getLogger(AuctionClient.class.getName());
  private static final Object networkLock = new Object();

  static {
    try (InputStream is = AuctionClient.class.getClassLoader().getResourceAsStream("logging.properties")) {
      if (is != null) {
        LogManager.getLogManager().readConfiguration(is);
        System.out.println("[CLIENT-LOGGER] Đã nạp cấu hình logging thành công!");
      }
    } catch (Exception e) {
      System.err.println("[CLIENT-LOGGER] Không thể nạp file cấu hình log: " + e.getMessage());
    }
  }

  private Socket socket;
  private BufferedReader in;
  private PrintWriter out;

  /** Thực hiện khởi tạo và kết nối Socket tới Server theo địa chỉ IP và Port chỉ định. */
  public void connect(String serverAddress, int port) {
    try {
      this.socket = new Socket(serverAddress, port);
      this.in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
      this.out = new PrintWriter(socket.getOutputStream(), true);
      logger.log(
              Level.INFO,
              "Đã kết nối thành công tới máy chủ đấu giá tại {0}:{1}",
              new Object[] {serverAddress, port});
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Gặp sự cố nghiêm trọng khi cố gắng mở cổng kết nối tới Server", e);
      throw new RuntimeException("Error connecting to server: " + e.getMessage(), e);
    }
  }

  /**
   * Gửi chuỗi tin nhắn JSON đồng bộ lên Server và chờ phản hồi một dòng văn bản ngược lại. Có tích
   * hợp bộ lọc tự động giải phóng dữ liệu kẹt trong bộ đệm (ống truyền mạng).
   */
  public String sendRequest(String requestJson) {
    synchronized (networkLock) {
      try {
        while (in.ready()) {
          String trash = in.readLine();
          logger.log(
                  Level.WARNING,
                  "Phát hiện và dọn dẹp dữ liệu rác kẹt trong ống truyền mạng: {0}",
                  trash);
        }
        out.println(requestJson);
        return in.readLine();
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Lỗi truyền thông Socket khi đang thực hiện sendRequest", e);
        throw new RuntimeException("Error sending request: " + e.getMessage(), e);
      }
    }
  }

  public PrintWriter getOut() {
    return this.out;
  }

  public BufferedReader getIn() {
    return this.in;
  }
}
