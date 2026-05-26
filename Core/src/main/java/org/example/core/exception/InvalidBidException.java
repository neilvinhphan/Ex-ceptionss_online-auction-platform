package org.example.core.exception;

public class InvalidBidException extends AuctionException {
    public InvalidBidException(String message) {
        super(message, 4003);
    }
}