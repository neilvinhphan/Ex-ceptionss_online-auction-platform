package org.example.server.services;

import org.example.core.dto.userDTO.SellerDashboardDTO;
import org.example.server.daos.DashboardDAO;

import java.util.Map;

public class DashBoardService {
    private final DashboardDAO dashboardDAO;
    private static volatile DashBoardService instance = null;

    DashBoardService(DashboardDAO dashboardDAO) {
        this.dashboardDAO = dashboardDAO;
    }

    public static DashBoardService getInstance() {
        if (instance == null) {
            synchronized (DashBoardService.class) {
                if (instance == null) {
                    instance = new DashBoardService(DashboardDAO.getInstance());
                }
            }
        }
        return instance;
    }

    public Map<String, String> getKPIs() {
        return dashboardDAO.getKPIs();
    }

    public Map<String, Integer> getCategoryDistribution() {
        return dashboardDAO.getCategoryDistribution();
    }

    public Map<String, Integer> getAuctionStatusDistribution() {
        return dashboardDAO.getAuctionStatusDistribution();
    }

    public SellerDashboardDTO getSellerDashboard(int sellerId) {
        return dashboardDAO.getSellerDashboardStats(sellerId);
    }
}
