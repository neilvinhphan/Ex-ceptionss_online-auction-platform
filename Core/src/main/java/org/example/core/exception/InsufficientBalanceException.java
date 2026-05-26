package org.example.core.exception;

public class InsufficientBalanceException extends AuctionException {
    public InsufficientBalanceException(String message) {
        super(message, 4001);
    }
}