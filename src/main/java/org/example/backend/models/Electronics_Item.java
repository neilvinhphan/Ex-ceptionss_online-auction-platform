package org.example.backend.models;

import java.time.LocalDateTime;

public class Electronics_Item extends Item {
    protected String brand;
    protected int warrantyMonths; //Số tháng bảo hành
    protected String condition; //tình trạng của sản phẩm

    public Electronics_Item(int id, String itemName, String brand, int warrantyMonths, String condition, String description, LocalDateTime startTime, LocalDateTime endTime, double startingPrice, double currentHightestBid) {
        super(id, itemName, description, startTime, endTime, startingPrice, currentHightestBid);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
        this.condition = condition;
    }


    public String getCondition() {
        return condition;
    }

    public String getBrand() {
        return brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void printInfo() {
        System.out.println("Mã sản phẩm" + id);
        System.out.println("Xe " + getItemName());
        System.out.println("Thương hiệu: " + brand);
        System.out.println("Số tháng bảo hành sản phẩm " + warrantyMonths);
        System.out.println("Tình trạng sản phẩm: " + condition);
        System.out.println("Mô tả sản phẩm: " + description);
        System.out.println("Thời gian bắt đầu đấu giá: " + startTime);
        System.out.println("Thời gian đóng phiên đấu giá: " + endTime);
        System.out.println("Giá khởi điểm: " + getStartingPrice());
        System.out.println("Giá hiên tại: " + currentHightestBid);


    }

}
