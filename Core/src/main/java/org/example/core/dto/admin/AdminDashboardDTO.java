package org.example.core.dto.admin;

import java.util.Map;

public class AdminDashboardDTO {
    private Map<String, String> kpis;
    private Map<String, Integer> categories;
    private Map<String, Integer> auctionStatus;

    public AdminDashboardDTO(Map<String, String> kpis, Map<String, Integer> categories, Map<String, Integer> auctionStatus) {
        this.kpis = kpis;
        this.categories = categories;
        this.auctionStatus = auctionStatus;
    }

    public Map<String, String> getKpis() { return kpis; }
    public Map<String, Integer> getCategories() { return categories; }
    public Map<String, Integer> getAuctionStatus() { return auctionStatus; }
}
