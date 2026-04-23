package org.example.server.services;

import org.example.core.models.entities.BidTransaction;
import org.example.core.shared.enums.AuctionStatus;
import org.example.server.daos.AuctionDAO;
import org.example.server.daos.BidDAO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BidService {
  private final BidDAO bidDAO;
  private final AuctionDAO auctionDAO;

  public BidService(BidDAO bidDAO, AuctionDAO auctionDAO) {
    this.bidDAO = bidDAO;
    this.auctionDAO = auctionDAO;
  }

  public BidService() {
    this(BidDAO.getInstance(), AuctionDAO.getInstance());
  }

  public void placeManualBid(int auctionId, int userId, BigDecimal bidAmount) throws Exception {
    if (auctionId <= 0) {
      throw new Exception("Invalid auction id.");
    }
    if (userId <= 0) {
      throw new Exception("Invalid user id.");
    }
    if (bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new Exception("Bid amount must be greater than zero.");
    }

    String status = auctionDAO.getAuctionStatus(auctionId);
    if (status == null || !AuctionStatus.RUNNING.name().equalsIgnoreCase(status)) {
      throw new Exception("Auction is not running.");
    }

    BigDecimal currentPrice = bidDAO.getCurrentPrice(auctionId);
    if (currentPrice != null && bidAmount.compareTo(currentPrice) <= 0) {
      throw new Exception("Bid amount must be greater than current price.");
    }

    boolean success = bidDAO.updateNewBid(auctionId, userId, bidAmount);
    if (!success) {
      throw new Exception("Cannot place bid.");
    }
  }

  public List<BidTransaction> getBidHistory(int auctionId) throws Exception {
    if (auctionId <= 0) {
      throw new Exception("Invalid auction id.");
    }
    return bidDAO.getBidTransactionByAuctionId(auctionId);
  }

  public int scanAndCloseExpiredAuctions() {
    List<Integer> expiredAuctionIds = auctionDAO.getExpiredRunningAuctionIds(LocalDateTime.now());
    for (Integer auctionId : expiredAuctionIds) {
      auctionDAO.setAuctionStatus(auctionId, AuctionStatus.FINISHED);
    }
    return expiredAuctionIds.size();
  }
}
