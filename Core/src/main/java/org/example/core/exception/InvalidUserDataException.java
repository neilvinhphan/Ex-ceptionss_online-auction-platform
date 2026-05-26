package org.example.core.exception;

public class InvalidUserDataException extends AuctionException {
    public InvalidUserDataException(String message) {
        super(message, 4000);
    }
}