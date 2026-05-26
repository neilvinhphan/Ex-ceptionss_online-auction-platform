package org.example.core.exception;

public class AuthenticationException extends AuctionException {
    public AuthenticationException(String message) {
        super(message, 4010);
    }
}