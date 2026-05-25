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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bộ quản lý hạ tầng mạng trung tâm ứng dụng mô hình Singleton pattern. Tích hợp sẵn bộ phân tích
 * cú pháp đa hình Gson (Polymorphic JSON Deserializer) dành cho các danh mục tài sản.
 */
public class ClientManager {

  private static final Logger logger = Logger.getLogger(ClientManager.class.getName());
  private static volatile ClientManager instance;

  private final AuctionClient client;
  private final Gson gson;

  private ClientManager() {
    this.client = new AuctionClient();
    this.gson =
        new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(
                Item.class,
                (JsonDeserializer<Item>)
                    (json, typeOfT, context) -> {
                      JsonObject jsonObject = json.getAsJsonObject();
                      String type =
                          jsonObject.has("type") ? jsonObject.get("type").getAsString() : "";
                      switch (type.toUpperCase()) {
                        case "ART":
                          return context.deserialize(jsonObject, ArtItem.class);
                        case "ELECTRONICS":
                          return context.deserialize(jsonObject, ElectronicsItem.class);
                        case "VEHICLE":
                          return context.deserialize(jsonObject, VehicleItem.class);
                        default:
                          logger.log(
                              Level.WARNING,
                              "Không nhận diện được nhãn loại danh mục tài sản lạ: {0}",
                              type);
                          throw new JsonParseException(
                              "Client không nhận diện được loại tài sản: " + type);
                      }
                    })
            .create();
  }

  /** Lấy thực thể duy nhất của ClientManager sử dụng Double-Checked Locking đảm bảo Thread-safe. */
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

  /** Ủy quyền kết nối Socket Client tới đích Server cấu hình. */
  public void connect(String serverAddress, int port) {
    try {
      getClient().connect(serverAddress, port);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Không thể thiết lập kết nối hạ tầng từ cổng ClientManager", e);
    }
  }
}
