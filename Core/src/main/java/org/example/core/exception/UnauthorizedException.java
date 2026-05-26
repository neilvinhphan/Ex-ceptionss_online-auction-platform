package org.example.core.exception;

public class UnauthorizedException extends AuctionException {
  public UnauthorizedException(String message) {
    super(message, 4030);
  }
}