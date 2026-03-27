package org.example.backend.models;

import java.time.LocalDateTime;

public class Vehicle_Item extends Item {
    protected String brand;
    protected String model;
    protected int manufacturingYear; // Năm sản xuất
    protected double mileage; // số km đã đi

    public Vehicle_Item(int id, String itemName, String brand, String model, int manufacturingYear, double mileage, String description, LocalDateTime startTime, LocalDateTime endTime, double startingPrice, double currentHightestBid) {
        super(id, itemName, description, startTime, endTime, startingPrice, currentHightestBid);
        this.brand = brand;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.mileage = mileage;
    }


    public double getMileage() {
        return mileage;
    }

    public int getManufacturingYear() {
        return manufacturingYear;
    }

    public String getModel() {
        return model;
    }

    public String getBrand() {
        return brand;
    }

    public void printInfo() {
        System.out.println("Mã sản phẩm" + id);
        System.out.println("Xe " + getItemName());
        System.out.println("Thương hiệu: " + brand);
        System.out.println("Năm sản xuất:" + manufacturingYear);
        System.out.println("Số km đã đi: " + mileage);
        System.out.println("Tổng quan về chiếc xe: " + description);
        System.out.println("Thời gian bắt đầu đấu giá: " + startTime);
        System.out.println("Thời gian đóng phiên đấu giá: " + endTime);
        System.out.println("Giá khởi điểm: " + getStartingPrice());
        System.out.println("Giá hiên tại: " + currentHightestBid);
    }

}
