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
 * Dịch vụ tích hợp trí tuệ nhân tạo (Groq API) để tự động kiểm duyệt nội dung
 * và gợi ý định giá tài sản khi người dùng đăng tải.
 */
public class AiModerationService {
  private static final Logger logger = Logger.getLogger(AiModerationService.class.getName());

  private static final String API_KEY = "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";
  private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

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

      String payload = """
              {
                "model": "llama-3.3-70b-versatile",
                "messages": [
                  {
                    "role": "user",
                    "content": %s
                  }
                ]
              }
              """.formatted(gson.toJson(prompt));

      HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(API_URL))
              .header("Authorization", "Bearer " + API_KEY)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(payload))
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return parseAiResponse(response.body());

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi kết nối hoặc thực thi yêu cầu kiểm duyệt tới Groq API", e);
      return new AiEvaluationDTO(true, item.getStartingPrice(), "AI tạm thời không khả dụng: " + e.getMessage());
    }
  }

  private static AiEvaluationDTO parseAiResponse(String responseBody) {
    logger.info("GROQ RAW RESPONSE: " + responseBody);
    try {
      JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);

      // Xử lý báo lỗi trực tiếp từ hệ thống API của Groq
      if (jsonObject.has("error")) {
        JsonObject errorObj = jsonObject.getAsJsonObject("error");
        String errMsg = errorObj.get("message").getAsString();
        logger.log(Level.WARNING, "Lỗi phản hồi cấu trúc từ hệ thống Groq API: " + errMsg);
        return new AiEvaluationDTO(true, null, "Lỗi API: " + errMsg);
      }

      String rawAiText = jsonObject
              .getAsJsonArray("choices").get(0).getAsJsonObject()
              .getAsJsonObject("message")
              .get("content").getAsString();

      String cleanJson = rawAiText.replaceAll("```json", "").replaceAll("```", "").trim();
      logger.info("CHUỖI JSON ĐÃ LÀM SẠCH: " + cleanJson);

      return gson.fromJson(cleanJson, AiEvaluationDTO.class);

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Thất bại khi bóc tách cú pháp phản hồi mã token từ Groq AI", e);
      return new AiEvaluationDTO(true, null, "Không thể giải mã phản hồi từ AI. Cần kiểm tra lại.");
    }
  }
}