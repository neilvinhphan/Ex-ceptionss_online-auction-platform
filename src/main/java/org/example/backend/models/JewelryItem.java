package org.example.backend.models;

import java.time.LocalDateTime;

public class JewelryItem extends Item {
    protected String material;       // Chất liệu chính
    protected String gemstone;       // Đá quý đính kèm
    protected double weight;         // Trọng lượng (Carat hoặc Gram)
    protected String certification;  // Giấy kiểm định

    public JewelryItem(int id, LocalDateTime createdAt, String type, String itemName, String material, String gemstone, double weight, String certification, String description, double startingPrice) {
        super(id, createdAt, type, itemName, description, startingPrice);
        this.material = material;
        this.gemstone = gemstone;
        this.weight = weight;
        this.certification = certification;
    }


    public String getMaterial() {
        return material;
    }

    public String getGemstone() {
        return gemstone;
    }

    public double getWeight() {
        return weight;
    }

    public String getCertification() {
        return certification;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public void setGemstone(String gemstone) {
        this.gemstone = gemstone;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void setCertification(String certification) {
        this.certification = certification;
    }

    @Override
    public void printInfo() {
        System.out.println("Mã sản phẩm: " + id);
        System.out.println("Trang sức: " + getItemName());
        System.out.println("Chất liệu: " + material);
        System.out.println("Đá quý: " + gemstone);
        System.out.println("Trọng lượng: " + weight + " (Carat/Gram)");
        System.out.println("Giấy kiểm định: " + certification);
        System.out.println("Mô tả chi tiết: " + description);
        System.out.println("Giá khởi điểm: " + getStartingPrice());
    }
}

