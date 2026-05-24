package org.example.client.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GroqAIService {

    // 🔥 Thay API Key của ông vào đây
    private static final String API_KEY = "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";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Dùng chung một HttpClient để tối ưu hiệu suất
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson gson = new Gson();

    /**
     * Gửi Prompt văn bản lên Groq và nhận về kết quả phân tích.
     */
    public static String analyzeMarket(String prompt) {
        try {
            // 1. Đóng gói dữ liệu đầu vào (Cấu trúc hệt như OpenAI)
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);

            JsonArray messages = new JsonArray();
            messages.add(message);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "llama-3.1-8b-instant");
            requestBody.add("messages", messages);
            requestBody.addProperty("temperature", 0.3); // Nhiệt độ thấp để AI trả lời khách quan, bớt bịa chuyện

            // 2. Tạo Request HTTP
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            // 3. Bắn Request lên Server Groq và chờ phản hồi
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 4. Bóc tách JSON lấy kết quả
            if (response.statusCode() == 200) {
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                return jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            } else {
                System.err.println("❌ Lỗi API Groq: " + response.body());
                return "Không thể phân tích dữ liệu lúc này (Lỗi: " + response.statusCode() + ")";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Đã xảy ra lỗi hệ thống khi kết nối AI: " + e.getMessage();
        }
    }
}