package org.example.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class AuctionClient {
    public static AuctionClient instance;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private static final Object networkLock = new Object();

    public void connect(String serverAddress, int port) {
        try {
            this.socket = new Socket(serverAddress, port);
            this.in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to auction server at " + serverAddress + ":" + port);
        } catch (IOException e) {
            throw new RuntimeException("Error connecting to server: " + e.getMessage(), e);
        }
    }

    public String sendRequest(String requestJson) {
        synchronized (networkLock) {
            try {
                while (in.ready()) {
                    String trash = in.readLine();
                    System.out.println("🚨 CẢNH BÁO: Đã dọn rác kẹt trong ống: " + trash);
                }
                out.println(requestJson);
                return in.readLine();
            } catch (IOException e) {
                throw new RuntimeException("Error sending request: " + e.getMessage(), e);
            }
        }
    }
        public PrintWriter getOut () {
            return this.out; // Trả về biến out đã khai báo ở trên cùng của class
        }

        public BufferedReader getIn () {
            return this.in; // Trả về biến in đã khai báo ở trên cùng của class
        }

}
