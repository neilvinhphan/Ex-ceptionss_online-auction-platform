package org.example.core.exception;

public class ItemNotFoundException extends AuctionException {
    public ItemNotFoundException(String message) {
        super(message, 4040);
    }
}