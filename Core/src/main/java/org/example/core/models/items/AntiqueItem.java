package org.example.core.models.items;

import java.math.BigDecimal;

public class AntiqueItem extends Item {
  private String era; // niên đại<xuất xứ>
  private String material;
  private String condition; // Tình trạng
  private boolean isCertified;

  public AntiqueItem() {
    super();
  }

  private AntiqueItem(Builder builder) {
    super(builder);
    this.era = builder.era;
    this.material = builder.material;
    this.condition = builder.condition;
    this.isCertified = builder.isCertified;
  }

  public static class Builder extends Item.Builder<Builder> {
    private String era;
    private String material;
    private String condition;
    private boolean isCertified;

    public Builder(int sellerID, String itemName, BigDecimal startingPrice) {
      super(sellerID, itemName, startingPrice);
    }

    public Builder era(String era) {
      this.era = era;
      return this;
    }

    public Builder material(String material) {
      this.material = material;
      return this;
    }

    public Builder condition(String condition) {
      this.condition = condition;
      return this;
    }

    public Builder certified(boolean certified) {
      this.isCertified = certified;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public AntiqueItem build() {
      return new AntiqueItem(this);
    }
  }

  @Override
  public String getType() {
    return "ANTIQUE";
  }

  public String getEra() {
    return era;
  }

  public String getMaterial() {
    return material;
  }

  public String getCondition() {
    return condition;
  }

  public void setEra(String era) {
    this.era = era;
  }

  public void setMaterial(String material) {
    this.material = material;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  public boolean isCertified() {
    return isCertified;
  }

  public void setCertified(boolean certified) {
    isCertified = certified;
  }
}
