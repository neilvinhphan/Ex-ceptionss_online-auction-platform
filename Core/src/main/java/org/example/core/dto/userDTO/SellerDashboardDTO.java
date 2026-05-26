package org.example.core.dto.userDTO;

import java.util.Map;

public class SellerDashboardDTO {
    private double totalRevenue;
    private int totalSold;
    private Map<String, Integer> categoryData;
    private Map<String, Double> revenueGrowthData;

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
    public double getTotalRevenue() { return totalRevenue; }
    public int getTotalSold() { return totalSold; }
    public Map<String, Integer> getCategoryData() { return categoryData; }
}