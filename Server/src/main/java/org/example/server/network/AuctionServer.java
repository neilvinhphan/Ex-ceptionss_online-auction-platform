package org.example.server.network;

import java.io.IOException;
import java.net.ServerSocket;

public class AuctionServer {
    private static final int PORT = 9000;

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Auction Server started on port " + PORT);
            while (true) {
                var clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error starting server: " + e.getMessage(), e);
        }
    }
}
