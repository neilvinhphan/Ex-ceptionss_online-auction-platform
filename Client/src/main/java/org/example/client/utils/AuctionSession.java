package org.example.client.utils;

import org.example.core.models.entities.Auction;
import org.example.core.models.items.Item;

public class AuctionSession {
  private static volatile AuctionSession instance;
  private Auction currentAuction;
  private Item currentItem;

  private AuctionSession() {}

  public static AuctionSession getInstance() {
    if (instance == null) {
      synchronized (AuctionSession.class) {
        if (instance == null) {
          instance = new AuctionSession();
        }
      }
    }
    return instance;
  }

  public void setRoomData(Auction auction, Item item) {
    this.currentAuction = auction;
    this.currentItem = item;
  }

  public Auction getCurrentAuction() {
    return currentAuction;
  }

  public Item getCurrentItem() {
    return currentItem;
  }

  public void setCurrentItem(Item currentItem) {
    this.currentItem = currentItem;
  }

  public void setCurrentAuction(Auction currentAuction) {
    this.currentAuction = currentAuction;
  }

  public void clearSession() {
    this.currentAuction = null;
    this.currentItem = null;
  }
}
