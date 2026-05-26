package org.example.core.exception;

public class DatabaseAccessException extends AuctionException {
    public DatabaseAccessException(String message, Throwable cause) {
        super(message, 5000, cause);
    }
    public DatabaseAccessException(String message) {
        super(message, 5000);
    }
}