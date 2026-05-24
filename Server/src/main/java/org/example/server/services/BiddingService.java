package org.example.server.services;

import org.example.core.dto.Response;
import org.example.core.dto.bidDTO.BidBroadcastDTO;
import org.example.core.dto.bidDTO.BidRequestDTO;
import org.example.core.models.entities.Auction;
import org.example.core.models.entities.BidTransaction;
import org.example.core.shared.enums.AuctionStatus;
import org.example.server.daos.AuctionDAO;
import org.example.server.daos.AutoBidDAO;
import org.example.server.daos.BidDAO;
import org.example.server.daos.UserDAO;
import org.example.server.daos.WalletDAO;
import org.example.server.network.AuctionServer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class BiddingService {

    private static volatile BiddingService instance;

    private final BidDAO bidDAO;
    private final AuctionDAO auctionDAO;
    private final WalletDAO walletDAO;
    private final UserDAO userDAO;
    private final AutoBidDAO autoBidDAO;

    // lock theo từng auction để tránh đè giá cùng lúc
    private ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    BiddingService(BidDAO bidDAO, AuctionDAO auctionDAO, WalletDAO walletDAO, UserDAO userDAO, AutoBidDAO autoBidDAO) {
        this.bidDAO = bidDAO;
        this.auctionDAO = auctionDAO;
        this.walletDAO = walletDAO;
        this.userDAO = userDAO;
        this.autoBidDAO = autoBidDAO;
    }

    public static BiddingService getInstance() {
        if (instance == null) {
            synchronized (BiddingService.class) {
                if (instance == null) {
                    instance = new BiddingService(
                            BidDAO.getInstance(),
                            AuctionDAO.getInstance(),
                            WalletDAO.getInstance(),
                            UserDAO.getInstance(),
                            AutoBidDAO.getInstance()
                    );
                }
            }
        }
        return instance;
    }

    private ReentrantLock getLock(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, id -> new ReentrantLock());
    }

    public boolean placeBid(BidRequestDTO request) throws Exception {
        if (request == null) {
            throw new Exception("Yêu cầu đặt giá không hợp lệ hoặc để trống!");
        }
        if (request.getAuctionId() <= 0) {
            throw new Exception("Mã phiên đấu giá được cung cấp không hợp lệ!");
        }
        if (request.getUserId() <= 0) {
            throw new Exception("Mã người dùng đặt giá không hợp lệ!");
        }
        if (request.getBidAmount() == null || request.getBidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Số tiền đấu giá đặt lên phải lớn hơn 0 VNĐ!");
        }

        String threadName = Thread.currentThread().getName();

        ReentrantLock lock = getLock(request.getAuctionId());
        lock.lock();
        try {
            System.out.println(
                    "[" + threadName + "] 🟢 Đã cầm chìa khóa (Lock)! Đang xử lý giá cho User " + request.getUserId());

            LocalDateTime now = LocalDateTime.now();

            Auction auction = auctionDAO.getAuctionByAuctionId(request.getAuctionId());
            if (auction == null) {
                throw new Exception("Không tìm thấy dữ liệu của phiên đấu giá này trên hệ thống.");
            }

            // check trạng thái + thời gian
            auction.validateBid(now, request.getBidAmount());

            // 1. Tải giá thầu cao nhất hiện tại từ bảng giao dịch
            BigDecimal currentPrice = bidDAO.getCurrentPrice(request.getAuctionId());
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                currentPrice = auction.getItem().getStartingPrice(); // Nếu chưa có ai đặt, lấy giá khởi điểm thực tế
            }

            // --- 🔥 FIX LỖI 2: NỚI LỎNG BIẾN VALIDATE CHẶN CHÍNH MÌNH ---
            // Kiểm tra xem người đặt hiện tại có phải người dẫn đầu không
            Integer highestBidderId = auction.getBidderId(); // Lấy ID người dẫn đầu phòng hiện tại

            // Nếu chính mình đang dẫn đầu, nhưng số tiền muốn đặt thủ công lớn hơn giá hiện tại -> Hợp lệ!
            if (highestBidderId != null && highestBidderId == request.getUserId()) {
                if (request.getBidAmount().compareTo(currentPrice) <= 0) {
                    throw new Exception("Bạn đang là người dẫn đầu phòng! Số tiền đặt mới phải lớn hơn giá hiện tại: " + currentPrice);
                }
            } else {
                // Nếu là người khác đặt, bọc hàm kiểm tra trạng thái thời gian phòng mặc định của ông
                auction.validateBid(now, request.getBidAmount());
            }

            // 2. Tính toán bước giá tối thiểu tiếp theo
            BigDecimal bidIncrement = auctionDAO.getBidIncrementByAuctionId(request.getAuctionId());
            if (bidIncrement == null || bidIncrement.compareTo(BigDecimal.ZERO) <= 0) {
                bidIncrement = BigDecimal.ONE;
            }

            // Xác định mức giá chấp nhận tối thiểu
            BigDecimal minAcceptable;
            BigDecimal checkDbPrice = bidDAO.getCurrentPrice(request.getAuctionId());
            if (checkDbPrice == null || checkDbPrice.compareTo(BigDecimal.ZERO) == 0) {
                // Nếu phòng trống trơn bóc tem: Người đầu tiên đặt bằng đúng giá khởi điểm là hợp lệ!
                minAcceptable = auction.getItem().getStartingPrice();
            } else {
                // Từ lượt thứ 2 trở đi bắt buộc phải >= Giá hiện tại + Bước giá
                minAcceptable = currentPrice.add(bidIncrement);
            }

            if (request.getBidAmount().compareTo(minAcceptable) < 0) {
                throw new Exception("Mức giá đặt thầu tối thiểu tiếp theo phải lớn hơn hoặc bằng: " + minAcceptable + " VNĐ");
            }

            // 3. Kiểm tra số dư ví
            BigDecimal availableBalance = walletDAO.getAvailableBalance(request.getUserId());
            if (request.getBidAmount().compareTo(availableBalance) > 0) {
                throw new Exception("Số dư khả dụng trong tài khoản ví của bạn không đủ để thực hiện lượt trả giá này!");
            }

            // 1. Lấy tên hiển thị của User đặt thầu tay từ cơ sở dữ liệu
            var user = userDAO.getUserByUserId(request.getUserId());
            if (user == null) {
                throw new Exception("Tài khoản người dùng đặt giá không tồn tại trên hệ thống!");
            }
            String bidderName = user.getUserName();

            // 2. Khởi tạo thực thể theo constructor 4 tham số có sẵn trong Core
            BidTransaction manualBidTx = new BidTransaction(request.getBidAmount(), now, request.getUserId(), bidderName);
            manualBidTx.setAuctionId(request.getAuctionId());

            boolean inserted = bidDAO.insertBid(manualBidTx);
            if (!inserted) {
                throw new Exception("Lỗi hệ thống: Không thể ghi nhận lịch sử lượt đặt giá mới vào cơ sở dữ liệu.");
            }

            boolean updatedPrice =
                    bidDAO.updateCurrentPrice(
                            request.getAuctionId(), request.getUserId(), request.getBidAmount());
            if (!updatedPrice) {
                throw new Exception("Lỗi hệ thống: Không thể cập nhật mức giá hiện tại dẫn đầu của phòng.");
            }

            handleAntiSniping(request.getAuctionId(), auction, now);

            System.out.println("[" + threadName + "] 💾 Ghi DB thành công cho User " + request.getUserId());
            return true;
        } catch (Exception e) {
            System.out.println("[" + threadName + "] ❌ Bị lỗi: " + e.getMessage());
            throw e;
        } finally {
            System.out.println("[" + threadName + "] 🔓 Trả lại chìa khóa (Unlock)!");
            lock.unlock();
        }
    }

    private void handleAntiSniping(int auctionId, Auction auction, LocalDateTime now) {
        LocalDateTime endTime = auction.getEndTime();
        if (endTime == null || !endTime.isAfter(now)) {
            return;
        }
        long antiSnipingSeconds = 120;
        boolean inSnipingWindow = auction.isAntiSniping(now, antiSnipingSeconds);
        if (!inSnipingWindow) return;
        auction.extendEndTime(antiSnipingSeconds);
        auctionDAO.updateAuctionEndTime(auctionId, auction.getEndTime());
    }

    public BigDecimal getMaxAutoBid(int auctionId, int userId) throws Exception {
        if (auctionId <= 0 || userId <= 0) {
            throw new Exception("Thông tin định danh phòng đấu giá hoặc người dùng bị sai.");
        }
        return autoBidDAO.getMaxAutoBid(auctionId, userId);
    }

    public void disableAutoBid(int auctionId, int userId) throws Exception {
        if (auctionId <= 0 || userId <= 0) {
            throw new Exception("Mã định danh không hợp lệ để hủy trạng thái đấu giá tự động.");
        }
        autoBidDAO.disableAutoBid(auctionId, userId);
    }

    public void saveOrUpdateAutoBid(int auctionId, int userId, BigDecimal maxBid) throws Exception {
        if (auctionId <= 0 || userId <= 0) {
            throw new Exception("Mã phòng hoặc mã người dùng thiết lập Robot tự động trả giá không hợp lệ.");
        }
        if (maxBid == null || maxBid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("Mức giá trần thiết lập tối đa cho Robot phải lớn hơn 0 VNĐ.");
        }
        BigDecimal availableBalance = walletDAO.getAvailableBalance(userId);
        if (maxBid.compareTo(availableBalance) > 0) {
            throw new Exception("Giá đặt trần tự động không được vượt quá số dư ví khả dụng hiện tại!");
        }
        autoBidDAO.saveOrUpdateAutoBid(auctionId, userId, maxBid);
    }

    public List<BidTransaction> getBidHistory(int auctionId) throws Exception {
        if (auctionId <= 0) {
            throw new Exception("Mã phiên đấu giá cần lấy lịch sử biểu đồ không hợp lệ!");
        }
        return bidDAO.getBidHistoryByAuctionId(auctionId);
    }

    public synchronized void evaluateDeterministicBidding(int auctionId) {
        try {
            Auction auction = auctionDAO.getAuctionByAuctionId(auctionId);
            if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) return;

            // 🔥 FIX TỐI THƯỢNG: Ép buộc lấy giá thật trực tiếp từ DB giao dịch
            BigDecimal highestBid = bidDAO.getCurrentPrice(auctionId);
            BigDecimal currentPrice;

            if (highestBid == null || highestBid.compareTo(BigDecimal.ZERO) <= 0) {
                currentPrice = auction.getItem().getStartingPrice();
            } else {
                currentPrice = highestBid;
            }

            BigDecimal increment = auction.getBidIncrement();

            List<AutoBidDAO.AutoBidConfig> activeBots =
                    autoBidDAO.getActiveAutoBidsForAuction(auctionId);

            if (activeBots.isEmpty()) return;

            AutoBidDAO.AutoBidConfig bot1 = activeBots.get(0);

            if (activeBots.size() >= 2) {
                // 🔥 BƯỚC LỌC THÔNG MINH: Tự phân loại Bot Trùm và Bot Nhì
                AutoBidDAO.AutoBidConfig highestBot = bot1.getMaxBid().compareTo(activeBots.get(1).getMaxBid()) >= 0 ? bot1 : activeBots.get(1);
                AutoBidDAO.AutoBidConfig secondBot  = bot1.getMaxBid().compareTo(activeBots.get(1).getMaxBid()) >= 0 ? activeBots.get(1) : bot1;

                // Trường hợp 1: Hai Bot ngang cơ nhau
                if (highestBot.getMaxBid().compareTo(secondBot.getMaxBid()) == 0) {
                    if (currentPrice.compareTo(highestBot.getMaxBid()) < 0) {
                        BigDecimal finalPrice = highestBot.getMaxBid();
                        int winnerId = highestBot.getCreatedAt().isBefore(secondBot.getCreatedAt()) ? highestBot.getUserId() : secondBot.getUserId();

                        if (finalPrice.compareTo(currentPrice) > 0 && auction.getBidderId() != winnerId) {
                            executeAutoBidTransaction(auctionId, finalPrice, winnerId, auction);
                        }
                    }
                }
                // Trường hợp 2: Bot Trùm tiền nhiều hơn Bot Nhì
                else {
                    BigDecimal baseComparePrice = secondBot.getMaxBid().compareTo(currentPrice) > 0 ? secondBot.getMaxBid() : currentPrice;
                    BigDecimal finalPrice = baseComparePrice.add(increment);

                    if (finalPrice.compareTo(highestBot.getMaxBid()) > 0) {
                        finalPrice = highestBot.getMaxBid();
                    }

                    if (finalPrice.compareTo(currentPrice) > 0 && auction.getBidderId() != highestBot.getUserId()) {
                        executeAutoBidTransaction(auctionId, finalPrice, highestBot.getUserId(), auction);
                    }
                }

            } else {
                // 🔥 FIX LỖI 1: Luồng xử lý cho 1 Bot cô đơn đặt giá thành công
                if (bot1.getMaxBid().compareTo(currentPrice) > 0 && auction.getBidderId() != bot1.getUserId()) {
                    BigDecimal finalPrice = currentPrice.add(increment);
                    if (finalPrice.compareTo(bot1.getMaxBid()) > 0) {
                        finalPrice = bot1.getMaxBid();
                    }

                    // Chốt hạ: Phải gọi hàm này thì Mockito mới bắt được transaction và DB mới lưu!
                    executeAutoBidTransaction(auctionId, finalPrice, bot1.getUserId(), auction);
                }
            }

            // =================================================================
            // 🔥 BƯỚC DỌN DẸP CHIẾN TRƯỜNG: TẮT BOT THUA CUỘC VÀ CHẠM TRẦN
            // =================================================================
            Auction checkAuction = auctionDAO.getAuctionByAuctionId(auctionId);
            if (checkAuction != null) {
                // 🔥 FIX: Lấy giá trực tiếp từ DB để kiểm tra điều kiện tắt Bot
                BigDecimal latestDbPrice = bidDAO.getCurrentPrice(auctionId);
                BigDecimal latestPrice = (latestDbPrice != null && latestDbPrice.compareTo(BigDecimal.ZERO) > 0)
                        ? latestDbPrice
                        : checkAuction.getItem().getStartingPrice();

                int currentLeaderId = checkAuction.getBidderId();

                for (AutoBidDAO.AutoBidConfig bot : activeBots) {
                    // TH1: Trần của Bot thấp hơn giá hiện tại (Hết tiền, thua đứt đuôi)
                    boolean isOutbid = bot.getMaxBid().compareTo(latestPrice) < 0;

                    // TH2: Trần của Bot bằng đúng giá hiện tại, nhưng bị người khác hớt tay trên (Chạm nóc nhưng vẫn thua)
                    boolean isMaxedOutAndLost = (bot.getMaxBid().compareTo(latestPrice) == 0 && bot.getUserId() != currentLeaderId);

                    if (isOutbid || isMaxedOutAndLost) {
                        // Gọi thẳng hàm có sẵn của bro luôn, cực kỳ OOP!
                        autoBidDAO.disableAutoBid(auctionId, bot.getUserId());
                        System.out.println("🤖 [AUTOBID CLEANUP] Đã tự động tắt Bot của User " + bot.getUserId() + " tại phòng " + auctionId);

                        // 2. 🔥 FIX UI: Bắn tín hiệu Socket ra toàn phòng để Client tự cập nhật nút bấm!
                        Response disableAlert = new Response("AUTOBID_DISABLED", "Bot chạm trần", bot.getUserId());
                        AuctionServer.broadcastToRoom(auctionId, disableAlert);
                    }
                }
            }
            // =================================================================
        } catch (Exception e) {
            System.err.println("❌ Lỗi xử lý lõi toán học AutoBid: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void executeAutoBidTransaction(int auctionId, BigDecimal finalPrice, int winnerId, Auction auction) throws Exception {
        LocalDateTime now = LocalDateTime.now(); // Lấy thời gian hiện tại
        String winnerName = userDAO.getUserByUserId(winnerId).getUserName();

        BidTransaction autoBidTx = new BidTransaction(finalPrice, now, winnerId, winnerName);
        autoBidTx.setAuctionId(auctionId);

        // 1. Lưu vào bảng lịch sử giao dịch
        bidDAO.insertBid(autoBidTx);

        // 2. Cập nhật giá và người dẫn đầu hiện tại (Lệnh chuẩn đã fix lúc nãy)
        bidDAO.updateCurrentPrice(auctionId, winnerId, finalPrice);

        // ==========================================================
        // 🔥 FIX LỖI Ở ĐÂY: KÍCH HOẠT ANTI-SNIPING CHO AUTOBID
        // ==========================================================
        // Gọi hàm cộng giờ trước khi báo cho Client biết
        handleAntiSniping(auctionId, auction, now);

        // 3. Đóng gói dữ liệu phát sóng. Lúc này auction.getEndTime() sẽ lấy được giờ MỚI NHẤT (nếu có cộng)
        BidBroadcastDTO broadcastData = new BidBroadcastDTO(
                auctionId,
                finalPrice.doubleValue(),
                winnerName,
                auction.getEndTime(),
                true
        );

        // 4. Phát loa cho cả phòng tự cập nhật giá và thời gian đếm ngược
        Response broadcastResponse = new Response("NEW_BID", "Hệ thống tự động đẩy giá!", broadcastData);
        AuctionServer.broadcastToRoom(auctionId, broadcastResponse);
        System.out.println("🚀 [AUTOBID BROADCAST] Giá phòng " + auctionId + " tự động tăng vọt lên " + finalPrice + " đ bởi Bot của " + winnerName);
    }
}