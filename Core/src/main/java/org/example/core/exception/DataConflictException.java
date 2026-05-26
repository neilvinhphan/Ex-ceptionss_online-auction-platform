package org.example.core.exception;

public class DataConflictException extends AuctionException {
    public DataConflictException(String message) {
        super(message, 4090);
    }
}