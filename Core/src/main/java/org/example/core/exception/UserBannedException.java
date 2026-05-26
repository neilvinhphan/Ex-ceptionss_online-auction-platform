package org.example.core.exception;

public class UserBannedException extends AuctionException {
    public UserBannedException(String message) {
        super(message, 4002);
    }
}