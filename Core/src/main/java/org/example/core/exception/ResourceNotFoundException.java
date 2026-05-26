package org.example.core.exception;

public class ResourceNotFoundException extends AuctionException {
    public ResourceNotFoundException(String message) {
        super(message, 4040);
    }
}