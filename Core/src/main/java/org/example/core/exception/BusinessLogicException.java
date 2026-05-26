package org.example.core.exception;

public class BusinessLogicException extends AuctionException {
    public BusinessLogicException(String message) {
        super(message, 4000);
    }
}