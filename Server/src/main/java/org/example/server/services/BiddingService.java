package org.example.server.services;

import org.example.core.models.entities.BidTransaction;
import org.example.core.shared.enums.AuctionStatus;
import org.example.server.daos.AuctionDAO;
import org.example.server.daos.BidDAO;

import java.math.BigDecimal;
import java.util.List;

public class BiddingService {
    private final BidDAO bidDAO;
    private final AuctionDAO auctionDAO;.


    public BiddingService(BidDAO bidDAO, AuctionDAO auctionDAO) {
        this.bidDAO = bidDAO.getInstance();
        this.auctionDAO = auctionDAO.getInstance();
    }

    // Đặt gi thủ công
    public void placeBid(int auctionId, int userId, BigDecimal bidAmount){
        if(auctionId <=0){
            throw new IllegalArgumentException("Invalid auction id");
        }
        if(userId <= 0 ){
            throw new IllegalArgumentException("Invalid user id");
        }

        if(bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) <=0) {
            throw  new IllegalArgumentException("Bid amount must be greater than zero");
        }

        String status = auctionDAO.getAuctionStatus(auctionId);
        if (status == null || !AuctionStatus.RUNNING.name().equalsIgnoreCase(status)){
            throw new IllegalArgumentException("Auction is not running");
        }

        BigDecimal currentPrice = bidDAO.getCurrentPrice(auctionId);
        if(currentPrice != null && bidAmount.compareTo(currentPrice) <= 0){
            throw new IllegalArgumentException("Bid amount must be greater than current price");
        }

        boolean success = bidDAO.updateNewBid(auctionId, userId, bidAmount);

        if(!success) {
            throw new IllegalStateException("Cannot place bid");
        }
    }

    public List<BidTransaction> getBidHistory(int auctionId){
        if(auctionId <= 0){
            throw new IllegalArgumentException("Invalid auction id");
        }
        return BidDAO.getInstance().getBidTransactionByAuctionId(auctionId);
    }

    public int scanAnd
}
