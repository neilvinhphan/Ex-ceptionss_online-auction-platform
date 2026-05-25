package org.example.client.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lớp dịch vụ tích hợp trí tuệ nhân tạo thông qua cổng API Groq Cloud.
 * Vận hành trên luồng HTTP Client hiện đại để phân tích xu hướng giá cả thị trường đấu giá.
 */
public class GroqAIService {

    private static final Logger logger = Logger.getLogger(GroqAIService.class.getName());
    private static final String API_KEY = "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";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson gson = new Gson();

    /**
     * Gửi Prompt văn bản lên mô hình Llama-3.1 xử lý ngữ nghĩa và trả về kết quả đúc kết thị trường.
     *
     * @param prompt Chuỗi cấu trúc dữ liệu thị trường và yêu cầu phân tích chuyên gia.
     * @return Chuỗi phân tích thị trường súc tích trả về từ AI.
     */
    public static String analyzeMarket(String prompt) {
        try {
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);

            JsonArray messages = new JsonArray();
            messages.add(message);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "llama-3.1-8b-instant");
            requestBody.add("messages", messages);
            requestBody.addProperty("temperature", 0.3);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                return jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            } else {
                logger.log(Level.SEVERE, "Yêu cầu API Groq thất bại với mã trạng thái: {0} - Chi tiết: {1}",
                        new Object[]{response.statusCode(), response.body()});
                return "Không thể phân tích dữ liệu lúc này (Lỗi: " + response.statusCode() + ")";
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Gặp sự cố lỗi hệ thống hoặc ngắt kết nối khi gửi yêu cầu lên Groq AI Server", e);
            return "Đã xảy ra lỗi hệ thống khi kết nối AI: " + e.getMessage();
        }
    }
}