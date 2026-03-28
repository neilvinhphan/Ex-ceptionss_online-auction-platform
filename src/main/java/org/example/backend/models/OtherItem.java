package org.example.backend.models;

import java.time.LocalDateTime;

public class OtherItem extends Item {
    protected String category; // Phân loại (Ví dụ: Nội thất, Quần áo, Thẻ bài sưu tầm...)
    protected String origin;   // Xuất xứ
    protected double weight;   // Trọng lượng (kg)

    public OtherItem(int id, String itemName, String description, LocalDateTime startTime, LocalDateTime endTime, double startingPrice, double currentHightestBid, String category, String origin, double weight) {
        super(id, itemName, description, startTime, endTime, startingPrice, currentHightestBid);
        this.category = category;
        this.origin = origin;
        this.weight = weight;
    }

    public String getCategory() {
        return category;
    }

    public String getOrigin() {
        return origin;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public void printInfo() {
        System.out.println("Mã sản phẩm: " + id);
        System.out.println("Sản phẩm khác: " + getItemName());
        System.out.println("Phân loại: " + category);
        System.out.println("Xuất xứ: " + origin);
        System.out.println("Trọng lượng: " + weight + " kg");
        System.out.println("Mô tả chi tiết: " + description);
        System.out.println("Thời gian bắt đầu: " + startTime);
        System.out.println("Thời gian đóng phiên: " + endTime);
        System.out.println("Giá khởi điểm: " + getStartingPrice());
        System.out.println("Giá hiện tại: " + currentHightestBid);
    }
}