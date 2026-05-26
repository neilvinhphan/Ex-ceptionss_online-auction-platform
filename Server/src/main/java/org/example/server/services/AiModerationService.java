package org.example.server.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.core.dto.admin.AiEvaluationDTO;
import org.example.core.models.items.Item;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dịch vụ tích hợp Groq (LLaMA-3.3) để tự động kiểm duyệt nội dung
 * và gợi ý định giá tài sản khi người dùng đăng tải.
 */
public class AiModerationService {
  private static final Logger logger = Logger.getLogger(AiModerationService.class.getName());

  private static final String API_KEY = "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";
  private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
  private static final String MODEL_NAME = "llama-3.3-70b-versatile";

  private static final Gson gson = new Gson();
  private static final HttpClient client = HttpClient.newHttpClient();

  private AiModerationService() {}

  public static AiEvaluationDTO evaluateItem(Item item) {
    try {
      String prompt = String.format(
              "You are an expert auction platform moderator. Analyze the product name: '%s' with description: '%s'. "
                      + "You MUST return a single, strict JSON object exactly like this format: "
                      + "{\"isSafe\": true/false, \"suggestedPrice\": number, \"reason\": \"string\"}. "
                      + "Set 'isSafe' to false if the product violates laws, weapons policies, or contains harmful/illegal keywords. "
                      + "Provide 'suggestedPrice' as a number representing its estimated market value in Vietnam (in VND). "
                      + "The 'reason' string MUST be written in English to explain your decision.",
              item.getItemName(), item.getDescription());

      // 1. Build messages array theo chuẩn OpenAI/Groq
      JsonObject message = new JsonObject();
      message.addProperty("role", "user");
      message.addProperty("content", prompt);

      JsonArray messages = new JsonArray();
      messages.add(message);

      // 2. Build root body request
      JsonObject body = new JsonObject();
      body.addProperty("model", MODEL_NAME);
      body.add("messages", messages);

      // 3. Ép model trả về định dạng JSON (JSON Mode của Groq)
      JsonObject responseFormat = new JsonObject();
      responseFormat.addProperty("type", "json_object");
      body.add("response_format", responseFormat);

      // 4. Set Header Authorization thay vì cộng chuỗi vào URL
      HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(API_URL))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + API_KEY)
              .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return parseAiResponse(response.body());

    } catch (Exception e) {
      logger.log(Level.SEVERE, "Lỗi kết nối hoặc thực thi yêu cầu kiểm duyệt tới Groq API", e);
      return new AiEvaluationDTO(true, item.getStartingPrice(), "AI tạm thời không khả dụng: " + e.getMessage());
    }
  }

  private static AiEvaluationDTO parseAiResponse(String responseBody) {
    logger.info("🤖 AI RAW RESPONSE: " + responseBody);
    try {
      JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);

      if (jsonObject.has("error")) {
        JsonObject errorObj = jsonObject.getAsJsonObject("error");
        String errMsg = errorObj.has("message") ? errorObj.get("message").getAsString() : "Unknown Error";
        logger.log(Level.WARNING, "❌ Lỗi từ hệ thống Groq API: " + errMsg);
        return new AiEvaluationDTO(true, null, "Lỗi API: " + errMsg);
      }

      // Bóc tách text theo chuẩn Groq (choices[0].message.content)
      String rawAiText = jsonObject
              .getAsJsonArray("choices").get(0).getAsJsonObject()
              .getAsJsonObject("message")
              .get("content").getAsString();

      // Dù đã dùng json_object mode, vẫn nên clean đề phòng
      String cleanJson = rawAiText.replaceAll("```json", "").replaceAll("```", "").trim();
      logger.info("🧼 CHUỖI JSON ĐÃ LÀM SẠCH: " + cleanJson);

      return gson.fromJson(cleanJson, AiEvaluationDTO.class);

    } catch (Exception e) {
      logger.log(Level.SEVERE, "❌ Thất bại khi bóc tách cú pháp phản hồi từ AI", e);
      return new AiEvaluationDTO(true, null, "Không thể giải mã phản hồi từ AI. Cần kiểm tra lại.");
    }
  }
}