package org.example.server.network;

import org.example.server.services.AuctionService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {
    private static final int PORT = 9000;
    ExecutorService executor = Executors.newFixedThreadPool(50);

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Auction Server started on port " + PORT);
            AuctionService.startAutoCloseJob();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                AuctionService.stopAutoCloseJob();
            }));
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                executor.execute(handler);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error starting server: " + e.getMessage(), e);
        }
    }
}
