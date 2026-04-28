package org.example.core.dto;

import java.math.BigDecimal;

public abstract class CreateItemRequestDTO {
    private String itemName;
    private String type;
   private BigDecimal startingPrice;
    private String description;

    public CreateItemRequestDTO(){}

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
public void setDescription(String description){
        this.description= description;
}
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

   // public BigDecimal getStartingPrice() {
   ///     return startingPrice;
 //   }

    public void setStartingPrice(BigDecimal startingPrice) {
       this.startingPrice = startingPrice;
    }
}
