-- Khởi tạo bảng auction (phiên đấu giá)
CREATE TABLE IF NOT EXISTS auction (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    item_name        VARCHAR(255) NOT NULL,
    item_description TEXT,
    status           ENUM('ACTIVE', 'ENDED', 'PENDING') NOT NULL DEFAULT 'ACTIVE',
    start_time       DATETIME,
    end_time         DATETIME,
    starting_price   DECIMAL(18, 0) NOT NULL DEFAULT 0,
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Khởi tạo bảng bid_transaction (lịch sử đặt giá)
CREATE TABLE IF NOT EXISTS bid_transaction (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    auction_id       INT NOT NULL,
    bidder_username  VARCHAR(255) NOT NULL,
    bid_amount       DECIMAL(18, 0) NOT NULL,
    bid_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auction FOREIGN KEY (auction_id) REFERENCES auction (id) ON DELETE CASCADE,
    CONSTRAINT fk_bidder  FOREIGN KEY (bidder_username) REFERENCES user (user_name)
);

-- Dữ liệu mẫu để kiểm thử
INSERT INTO auction (item_name, item_description, status, start_time, end_time, starting_price)
VALUES
    ('Đồng hồ Rolex Submariner', 'Đồng hồ lặn cao cấp, sản xuất năm 2020, còn bảo hành', 'ACTIVE',
     NOW(), DATE_ADD(NOW(), INTERVAL 3 DAY), 80000000),
    ('Tranh sơn dầu "Hoàng hôn biển"', 'Tranh gốc của họa sĩ Nguyễn Văn Minh, kích thước 80x120cm', 'ACTIVE',
     NOW(), DATE_ADD(NOW(), INTERVAL 2 DAY), 15000000),
    ('Xe Honda CB650R 2023', 'Xe mới 99%, chạy 500km, đầy đủ giấy tờ', 'ACTIVE',
     NOW(), DATE_ADD(NOW(), INTERVAL 5 DAY), 230000000);
