package org.example.core.models.items;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OtherItem extends Item {
  private String category;     // Phân loại (ví dụ: Nội thất, Quần áo...)
  private String origin;       // Xuất xứ
  private double weight;       // Trọng lượng (kg)

  // Giữ lại để tương thích luồng map từ DB hiện tại (nếu đang dùng reflection/new rỗng)
  public OtherItem() {
    super();
  }

  private OtherItem(Builder builder) {
    super(builder);
    this.category = builder.category;
    this.origin = builder.origin;
    this.weight = builder.weight;
  }

  @Override
  public String getType() {
    return "OTHER";
  }

  public String getCategory() {
    return category;
  }

  public String getOrigin() {
    return origin;
  }

  public double getWeight() {
    return weight;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public void setWeight(double weight) {
    this.weight = weight;
  }

  public static class Builder extends Item.Builder<Builder> {
    private String category;
    private String origin;
    private double weight;

    public Builder(int sellerID, String itemName, java.math.BigDecimal startingPrice) {
      super(sellerID, itemName, startingPrice);
    }

    public Builder category(String category) {
      this.category = category;
      return this;
    }

    public Builder origin(String origin) {
      this.origin = origin;
      return this;
    }

    public Builder weight(double weight) {
      this.weight = weight;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public OtherItem build() {
      return new OtherItem(this);
    }
  }
}
