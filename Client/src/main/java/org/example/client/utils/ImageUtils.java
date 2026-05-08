package org.example.client.utils;

import javafx.scene.image.Image;
import java.io.ByteArrayInputStream;
import java.util.Base64;

public class ImageUtils {
  public static Image decodeBase64ToImage(String base64String) {
    try {
      if (base64String == null || base64String.isEmpty()) {
        return null;
      }
      // 1. Loại bỏ tiền tố nếu có (ví dụ: data:image/png;base64,)
      String pureBase64 = base64String.contains(",") ? base64String.split(",")[1] : base64String;

      // 2. Giải mã chuỗi Base64 thành mảng byte
      byte[] imageBytes = Base64.getDecoder().decode(pureBase64);

      // 3. Chuyển mảng byte thành Image của JavaFX
      return new Image(new ByteArrayInputStream(imageBytes));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
