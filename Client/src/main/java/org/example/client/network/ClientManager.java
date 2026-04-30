package org.example.client.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.core.network.LocalDateTimeAdapter;
import java.time.LocalDateTime;

public class ClientManager {
    private static volatile ClientManager instance;
    private final AuctionClient client;
    private final Gson gson;

    public ClientManager() {
        this.client = new AuctionClient();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    public static ClientManager getInstance() {
        if (instance == null) {
            synchronized (ClientManager.class) {
                if (instance == null) {
                    instance = new ClientManager();
                }
            }
        }
        return instance;
    }

    public synchronized AuctionClient getClient() {
        return client;
    }

    public Gson getGson() {
        return gson;
    }

    public void connect(String serverAddress, int port) {
        try {
            getClient().connect(serverAddress, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
