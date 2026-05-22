package org.example.server.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.core.dto.admin.AiEvaluationDTO;
import org.example.core.models.items.Item;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;

public class AiModerationService {

  private static final String API_KEY = "AIzaSyBxIwBTSlKhzBcl3WEVhc2ej_gllLekuIw";
  private static final String API_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
          + API_KEY;
  private static final Gson gson = new Gson();
  private static final HttpClient client = HttpClient.newHttpClient();

  public static AiEvaluationDTO evaluateItem(Item item) {
    try {
      // 1. Xây dựng Prompt "huấn luyện" AI
      String prompt =
          String.format(
              "You are an expert auction platform moderator. Analyze the product name: '%s' with description: '%s'. "
                  + "You MUST return a single, strict, raw JSON object exactly like this format: "
                  + "{\"isSafe\": true/false, \"suggestedPrice\": number, \"reason\": \"string\"}. "
                  + "Set 'isSafe' to false if the product violates laws, weapons policies, or contains harmful/illegal keywords. "
                  + "Provide 'suggestedPrice' as a number representing its estimated market value in Vietnam (in VND). "
                  + "The 'reason' string MUST be written in English to explain your decision. "
                  + "Do NOT wrap the response in markdown blocks like ```json ... ``` and do NOT include any introductory text or conversational filler.",
              item.getItemName(), item.getDescription());

      // 2. Đóng gói Body theo chuẩn Google Gemini
      JsonObject content = new JsonObject();
      JsonObject part = new JsonObject();
      part.addProperty("text", prompt);
      content.add("parts", gson.toJsonTree(new JsonObject[] {part}));

      JsonObject body = new JsonObject();
      body.add("contents", gson.toJsonTree(new JsonObject[] {content}));

      // 3. Gửi Request
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(API_URL))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      // 4. Parse kết quả (Gemini trả về cấu trúc phức tạp, cần bóc tách lấy chuỗi JSON ở giữa)
      // Lưu ý: Đoạn này cần xử lý chuỗi JSON mà AI trả về trong field 'text'
      return parseAiResponse(response.body());

    } catch (Exception e) {
      e.printStackTrace();
      return new AiEvaluationDTO(
          true, item.getStartingPrice(), "AI tạm thời không khả dụng: " + e.getMessage());
    }
  }

  private static AiEvaluationDTO parseAiResponse(String responseBody) {
    System.out.println("🤖 AI RAW RESPONSE: " + responseBody);
    try {
      JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);

      // 1. Chốt chặn phòng thủ: Nếu Google trả về lỗi cấu trúc (như lỗi 404, hết quota...)
      if (jsonObject.has("error")) {
        JsonObject errorObj = jsonObject.getAsJsonObject("error");
        String errMsg = errorObj.get("message").getAsString();
        System.err.println("❌ Lỗi từ hệ thống Google API: " + errMsg);
        // Cơ chế fallback an toàn: Tự động cho qua để hệ thống không bị nghẽn, Admin duyệt tay sau
        return new AiEvaluationDTO(true, null, "Lỗi API: " + errMsg);
      }

      // 2. Bóc tách ma trận JSON của Gemini theo đúng cấu trúc: candidates[0].content.parts[0].text
      String rawAiText =
          jsonObject
              .getAsJsonArray("candidates")
              .get(0)
              .getAsJsonObject()
              .getAsJsonObject("content")
              .getAsJsonArray("parts")
              .get(0)
              .getAsJsonObject()
              .get("text")
              .getAsString();

      // 3. Làm sạch chuỗi text: Loại bỏ các ký tự bọc khối mã của Markdown (```json ... ```) nếu AI
      // tự ý thêm vào
      String cleanJson = rawAiText.replaceAll("```json", "").replaceAll("```", "").trim();

      System.out.println("🧼 CHUỖI JSON ĐÃ LÀM SẠCH: " + cleanJson);

      // 4. Ép chuỗi JSON sạch này vào DTO cốt lõi của hệ thống
      return gson.fromJson(cleanJson, AiEvaluationDTO.class);

    } catch (Exception e) {
      System.err.println("❌ Thất bại khi bóc tách cú pháp phản hồi từ AI: " + e.getMessage());
      // Fallback cứu hộ: Giữ an toàn cho luồng chính, chuyển thẳng về chế độ duyệt thủ công
      return new AiEvaluationDTO(true, null, "Không thể giải mã phản hồi từ AI. Cần kiểm tra lại.");
    }
  }
}
