package org.example.core.models.items;

import java.math.BigDecimal;

public class ArtItem extends Item {
  private String artist;
  private int creationYear;

  public ArtItem() {
    super();
  }

  private ArtItem(Builder builder) {
    super(builder);
    this.artist = builder.artist;
    this.creationYear = builder.creationYear;
  }

  public String getArtist() {
    return artist;
  }

  public int getCreationYear() {
    return creationYear;
  }

  public void setArtist(String artist) {
    this.artist = artist;
  }

  public void setCreationYear(int creationYear) {
    this.creationYear = creationYear;
  }

  public static class Builder extends Item.Builder<Builder> {
    private String artist;
    private int creationYear;

    public Builder(int sellerID, String itemName, BigDecimal startingPrice) {
      super(sellerID, itemName, startingPrice);
    }

    public Builder artist(String artist) {
      this.artist = artist;
      return this;
    }

    public Builder creationYear(int creationYear) {
      this.creationYear = creationYear;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public ArtItem build() {
      return new ArtItem(this);
    }
  }

  @Override
  public String getType() {
    return "ART";
  }
}
