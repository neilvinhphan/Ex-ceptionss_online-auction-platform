package org.example.backend.models.items;

import org.example.backend.models.Item;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RealEstateItem extends Item {
    protected String location;      // Vị trí/Địa chỉ
    protected double area;          // Diện tích (m2)
    protected String propertyType;  // Loại hình (Đất nền, Chung cư...)
    protected String legalStatus;   // Tình trạng pháp lý (Sổ đỏ, HĐMB...)

    public RealEstateItem(int id, LocalDateTime createdAt, String type, String itemName, String location, double area, String propertyType, String legalStatus, String description, BigDecimal startingPrice) {
        super(id, createdAt, type, itemName, description, startingPrice);
        this.location = location;
        this.area = area;
        this.propertyType = propertyType;
        this.legalStatus = legalStatus;
    }


    public String getLocation() {
        return location;
    }

    public double getArea() {
        return area;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public String getLegalStatus() {
        return legalStatus;
    }

    @Override
    public void printInfo() {
        System.out.println("Mã sản phẩm: " + id);
        System.out.println("Bất động sản: " + getItemName());
        System.out.println("Loại hình: " + propertyType);
        System.out.println("Vị trí: " + location);
        System.out.println("Diện tích: " + area + " m2");
        System.out.println("Pháp lý: " + legalStatus);
        System.out.println("Mô tả chi tiết: " + description);
        System.out.println("Giá khởi điểm: " + getStartingPrice());
    }
}
