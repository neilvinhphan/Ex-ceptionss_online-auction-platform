package org.example.client.network;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ClientManager {
    private static final Logger logger = Logger.getLogger(ClientManager.class.getName());
    private static volatile ClientManager instance;
    private static final ReentrantLock lock = new ReentrantLock();
    private final AuctionClient client;

    public ClientManager() {
        this.client = new AuctionClient();
    }

    public static ClientManager getInstance() {
        if (instance == null) {
            lock.lock();
            try {
                if (instance == null) {
                    instance = new ClientManager();
                }
            } finally {
                lock.unlock();
            }
        }
        return instance;
    }

    public synchronized AuctionClient getClient() {
        return client;
    }

    public void connect(String serverAddress, int port) {
        try {
            getClient().connect(serverAddress, port);
            logger.info("ClientManager: Connected to " + serverAddress + ":" + port);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "ClientManager: Connection failed", e);
            throw e;
        }
    }
}
