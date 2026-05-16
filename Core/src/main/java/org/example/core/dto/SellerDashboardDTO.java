package org.example.core.dto.userDTO;

import java.util.Map;

public class SellerDashboardDTO {
    private double totalRevenue;
    private int totalSold;
    private Map<String, Integer> categoryData;

    public SellerDashboardDTO(double totalRevenue, int totalSold, Map<String, Integer> categoryData) {
        this.totalRevenue = totalRevenue;
        this.totalSold = totalSold;
        this.categoryData = categoryData;
    }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
    public int getTotalSold() { return totalSold; }
    public void setTotalSold(int totalSold) { this.totalSold = totalSold; }
    public Map<String, Integer> getCategoryData() { return categoryData; }
    public void setCategoryData(Map<String, Integer> categoryData) { this.categoryData = categoryData; }
}