package org.example.server.services;

import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.server.daos.AuctionDAO;
import org.example.server.daos.BidDAO;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class BiddingService {

    private static volatile BiddingService instance;

    private static final BidDAO bidDAO = BidDAO.getInstance();
    private static final AuctionDAO auctionDAO = AuctionDAO.getInstance();

    // lock theo từng auction để tránh đè giá cùng lúc
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

//    BiddingService() {
//        this(BidDAO.getInstance(), AuctionDAO.getInstance());
//    }
//
//    BiddingService(BidDAO bidDAO, AuctionDAO auctionDAO) {
//        this.bidDAO = Objects.requireNonNull(bidDAO, "bidDAO must not be null");
//        this.auctionDAO = Objects.requireNonNull(auctionDAO, "auctionDAO must not be null");
//    }

    public static BiddingService getInstance() {
        if (instance == null) {
            synchronized (BiddingService.class) {
                if (instance == null) {
                    instance = new BiddingService();
                }
            }
        }
        return instance;
    }

    private ReentrantLock getLock(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, id -> new ReentrantLock());
    }

    /**
     * placeBid (cốt lõi):
     * 1) lock theo auction
     * 2) validate domain
     * 3) validate amount >= current + increment
     * 4) ghi DB bid + cập nhật current price
     * 5) anti sniping
     */
    public boolean placeBid(int auctionId, int userId, BigDecimal amount) throws Exception {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            LocalDateTime now = LocalDateTime.now();

            Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
            if (auction == null) {
                throw new Exception("Không tìm thấy phiên đấu giá.");
            }

            // check trạng thái + thời gian (đang có sẵn trong domain Auction)
            auction.validateBid(now, amount);

            BigDecimal currentPrice = bidDAO.getCurrentPrice(auctionId);
            if (currentPrice == null) {
                currentPrice = BigDecimal.ZERO;
            }

            BigDecimal bidIncrement = auctionDAO.getBidIncrementByAuctionId(auctionId);
            if (bidIncrement == null || bidIncrement.compareTo(BigDecimal.ZERO) <= 0) {
                bidIncrement = BigDecimal.ONE;
            }

            BigDecimal minAcceptable = currentPrice.add(bidIncrement);
            if (amount.compareTo(minAcceptable) < 0) {
                throw new Exception("Giá đặt phải >= " + minAcceptable);
            }

            boolean inserted = bidDAO.updateNewBid(auctionId, userId, amount);
            if (!inserted) {
                throw new Exception("Không thể ghi nhận lượt đặt giá.");
            }

            boolean updatedPrice = bidDAO.updateCurrentPrice(auctionId, amount);
            if (!updatedPrice) {
                throw new Exception("Không thể cập nhật giá hiện tại.");
            }

            handleAntiSniping(auctionId, auction, now);

            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * handleAntiSniping:
     * nếu bid trong 1/10 thời gian cuối thì cộng thêm 1/10 thời lượng phiên
     */
    private void handleAntiSniping(int auctionId, Auction auction, LocalDateTime now) {
        LocalDateTime startTime = auction.getStartTime();
        LocalDateTime endTime = auction.getEndTime();
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            return;
        }

        long totalSeconds = Duration.between(startTime, endTime).getSeconds();
        long antiSnipingSeconds = Math.max(1, totalSeconds / 10);

        boolean inSnipingWindow = auction.isAntiSniping(now, antiSnipingSeconds);
        if (!inSnipingWindow) return;

        auction.extendEndTime(antiSnipingSeconds);

        // TODO: bạn cần method update end_time trong AuctionDAO
        auctionDAO.updateAuctionEndTime(auctionId, auction.getEndTime());
    }

    /**
     * Lấy lịch sử để FE vẽ chart
     */
    public List<BidTransaction> getBidHistory(int auctionId) {
        return bidDAO.getBidTransactionByAuctionId(auctionId);
    }
}
