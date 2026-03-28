package org.example.backend.models;

import java.time.LocalDateTime;

public class AntiqueItem extends Item {
    protected String era;   // niên đại<xuất xứ>
    protected String material;
    protected String condition; //Tình trạng
    protected boolean isCertified;


    public AntiqueItem(int id, String itemName, String description, LocalDateTime startTime, LocalDateTime endTime, double startingPrice, double currentHightestBid, String era, String material, String condition, boolean isCertified) {
        super(id, itemName, description, startTime, endTime, startingPrice, currentHightestBid);
        this.era = era;
        this.material = material;
        this.condition = condition;
        this.isCertified = isCertified;
    }

    public String getEra() {
        return era;
    }

    public String getMaterial() {
        return material;
    }

    public String getCondition() {
        return condition;
    }

    public boolean isCertified() {
        return isCertified;
    }

    @Override
    public void printInfo() {
        System.out.println("Mã sản phẩm: " + id);
        System.out.println("Đồ cổ: " + getItemName());
        System.out.println("Niên đại/Xuất xứ: " + era);
        System.out.println("Chất liệu: " + material);
        System.out.println("Tình trạng bảo quản: " + condition);
        System.out.println("Giấy thẩm định: " + (isCertified ? "Đã có chứng nhận chuyên gia" : "Chưa qua thẩm định"));
        System.out.println("Mô tả chi tiết: " + description);
        System.out.println("Thời gian bắt đầu: " + startTime);
        System.out.println("Thời gian đóng phiên: " + endTime);
        System.out.println("Giá khởi điểm: " + getStartingPrice());
        System.out.println("Giá hiện tại: " + currentHightestBid);
    }
}
