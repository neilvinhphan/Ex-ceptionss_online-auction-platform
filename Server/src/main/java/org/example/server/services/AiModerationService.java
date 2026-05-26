package org.example.server.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.core.dto.admin.AiEvaluationDTO;
import org.example.core.exception.DatabaseAccessException;
import org.example.core.models.items.Item;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dịch vụ tích hợp trí tuệ nhân tạo (Google Gemini API) để tự động kiểm duyệt nội dung
 * và gợi ý định giá tài sản khi người dùng đăng tải.
 */
public class AiModerationService {
  private static final Logger logger = Logger.getLogger(AiModerationService.class.getName());
  private static final String API_KEY = "AIzaSyBxIwBTSlKhzBcl3WEVhc2ej_gllLekuIw";
  private static final String API_URL =
          "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

  private static final Gson gson = new Gson();
  private static final HttpClient client = HttpClient.newHttpClient();

  private AiModerationService() {}

  /**
   * Phân tích, thẩm định mức độ an toàn và ước tính giá trị thị trường của vật phẩm thông qua AI.
   */
  public static AiEvaluationDTO evaluateItem(Item item) {
    try {
      String prompt = String.format(
              "You are an expert auction platform moderator. Analyze the product name: '%s' with description: '%s'. "
                      + "You MUST return a single, strict, raw JSON object exactly like this format: "
                      + "{\"isSafe\": true/false, \"suggestedPrice\": number, \"reason\": \"string\"}. "
                      + "Set 'isSafe' to false if the product violates laws, weapons policies, or contains harmful/illegal keywords. "
                      + "Provide 'suggestedPrice' as a number representing its estimated market value in Vietnam (in VND). "
                      + "The 'reason' string MUST be written in English to explain your decision. "
                      + "Do NOT wrap the response in markdown blocks like ```json ... ``` and do NOT include any introductory text or conversational filler.",
              item.getItemName(), item.getDescription());

      JsonObject content = new JsonObject();
      JsonObject part = new JsonObject();
      part.addProperty("text", prompt);
      content.add("parts", gson.toJsonTree(new JsonObject[] {part}));

      JsonObject body = new JsonObject();
      body.add("contents", gson.toJsonTree(new JsonObject[] {content}));

      HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(API_URL))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return parseAiResponse(response.body());

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi kết nối hoặc thực thi yêu cầu kiểm duyệt tới Gemini API", e);
      return new AiEvaluationDTO(true, item.getStartingPrice(), "AI tạm thời không khả dụng: " + e.getMessage());
    }
  }

  private static AiEvaluationDTO parseAiResponse(String responseBody) {
    logger.info("AI RAW RESPONSE: " + responseBody);
    try {
      JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);

      if (jsonObject.has("error")) {
        JsonObject errorObj = jsonObject.getAsJsonObject("error");
        String errMsg = errorObj.get("message").getAsString();
        logger.log(Level.WARNING, "Lỗi phản hồi cấu trúc từ hệ thống Google API: " + errMsg);
        return new AiEvaluationDTO(true, null, "Lỗi API: " + errMsg);
      }

      String rawAiText = jsonObject
              .getAsJsonArray("candidates").get(0).getAsJsonObject()
              .getAsJsonObject("content")
              .getAsJsonArray("parts").get(0).getAsJsonObject()
              .get("text").getAsString();

      String cleanJson = rawAiText.replaceAll("```json", "").replaceAll("```", "").trim();
      logger.info("CHUỖI JSON ĐÃ LÀM SẠCH: " + cleanJson);

      return gson.fromJson(cleanJson, AiEvaluationDTO.class);

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Thất bại khi bóc tách cú pháp phản hồi mã token từ AI", e);
      return new AiEvaluationDTO(true, null, "Không thể giải mã phản hồi từ AI. Cần kiểm tra lại.");
    }
  }
}