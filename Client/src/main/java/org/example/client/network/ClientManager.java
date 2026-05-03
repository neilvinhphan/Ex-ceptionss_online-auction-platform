package org.example.client.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;
import org.example.core.network.LocalDateTimeAdapter;
import java.time.LocalDateTime;

public class ClientManager {
    private static volatile ClientManager instance;
    private final AuctionClient client;
    private final Gson gson;
/*
    public ClientManager() {
        this.client = new AuctionClient();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }*/
// SỬA public THÀNH private ĐỂ CHUẨN SINGLETON
private ClientManager() {
    this.client = new AuctionClient();

    // DẠY CHO GSON Ở CLIENT CÁCH ĐỌC ABSTRACT CLASS ITEM
    this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(
                    Item.class,
                    (JsonDeserializer<Item>) (json, typeOfT, context) -> {
                        JsonObject jsonObject = json.getAsJsonObject();
                        String type = jsonObject.has("type") ? jsonObject.get("type").getAsString() : "";
                        switch (type.toUpperCase()) {
                            case "ART":
                                return context.deserialize(jsonObject, ArtItem.class);
                            case "ELECTRONICS":
                                return context.deserialize(jsonObject, ElectronicsItem.class);
                            case "VEHICLE":
                                return context.deserialize(jsonObject, VehicleItem.class);
                            default:
                                throw new JsonParseException("Client không nhận diện được loại tài sản: " + type);
                        }
                    })
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
