package org.example.core.exception;

/**
 * Lớp ngoại lệ cơ sở (Base Exception) cho toàn bộ hệ thống đấu giá.
 * Kế thừa từ RuntimeException để tránh việc bắt buộc phải try-catch hoặc throws ở mọi nơi.
 */
public abstract class AuctionException extends RuntimeException {
    private final int errorCode;

    /**
     * Khởi tạo một ngoại lệ hệ thống mới.
     */
    public AuctionException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Khởi tạo một ngoại lệ hệ thống có kèm theo nguyên nhân gốc (Root cause).
     */
    public AuctionException(String message, int errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}