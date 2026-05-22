package org.example.core.dto.userDTO;

import java.util.Map;

public class SellerDashboardDTO {
    private double totalRevenue;
    private int totalSold;
    private Map<String, Integer> categoryData;
    private Map<String, Double> revenueGrowthData; // Khóa là mã/tên đơn hàng, Giá trị là doanh thu lũy kế hoặc doanh thu đơn đó

    // Constructor đầy đủ tham số
    public SellerDashboardDTO(double totalRevenue, int totalSold,
                              Map<String, Integer> categoryData,
                              Map<String, Double> revenueGrowthData) {
        this.totalRevenue = totalRevenue;
        this.totalSold = totalSold;
        this.categoryData = categoryData;
        this.revenueGrowthData = revenueGrowthData;
    }

    // Getter và Setter
    public Map<String, Double> getRevenueGrowthData() { return revenueGrowthData; }
    public void setRevenueGrowthData(Map<String, Double> revenueGrowthData) { this.revenueGrowthData = revenueGrowthData; }
    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
    public int getTotalSold() { return totalSold; }
    public void setTotalSold(int totalSold) { this.totalSold = totalSold; }
    public Map<String, Integer> getCategoryData() { return categoryData; }
    public void setCategoryData(Map<String, Integer> categoryData) { this.categoryData = categoryData; }
}