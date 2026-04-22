package org.example.core.models.items;

import java.math.BigDecimal;

public class JewelryItem extends Item {
  private String material; // Chất liệu chính
  private String gemstone; // Đá quý đính kèm
  private double weight; // Trọng lượng (Carat hoặc Gram)
  private String certification; // Giấy kiểm định

  public JewelryItem() {
    super();
  }

  private JewelryItem(Builder builder) {
    super(builder);
    this.material = builder.material;
    this.gemstone = builder.gemstone;
    this.weight = builder.weight;
    this.certification = builder.certification;
  }

  public static class Builder extends Item.Builder<Builder> {
    private String material; // Chất liệu chính
    private String gemstone; // Đá quý đính kèm
    private double weight; // Trọng lượng (Carat hoặc Gram)
    private String certification; // Giấy kiểm định

    public Builder(int sellerID, String itemName, BigDecimal startingPrice) {
      super(sellerID, itemName, startingPrice);
    }

    public Builder material(String material) {
      this.material = material;
      return this;
    }

    public Builder gemstone(String gemstone) {
      this.gemstone = gemstone;
      return this;
    }

    public Builder weight(double weight) {
      this.weight = weight;
      return this;
    }

    public Builder certification(String certification) {
      this.certification = certification;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public JewelryItem build() {
      return new JewelryItem(this);
    }
  }

  @Override
  public String getType() {
    return "JEWELRY";
  }

  public String getMaterial() {
    return material;
  }

  public String getGemstone() {
    return gemstone;
  }

  public double getWeight() {
    return weight;
  }

  public String getCertification() {
    return certification;
  }

  public void setMaterial(String material) {
    this.material = material;
  }

  public void setGemstone(String gemstone) {
    this.gemstone = gemstone;
  }

  public void setWeight(double weight) {
    this.weight = weight;
  }

  public void setCertification(String certification) {
    this.certification = certification;
  }
}
