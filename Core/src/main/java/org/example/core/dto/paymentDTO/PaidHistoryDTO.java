package org.example.core.dto.paymentDTO;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaidHistoryDTO implements Serializable {
    private String itemName;
    private String category;
    private BigDecimal finalPrice;
    private LocalDateTime paidDate;

    public PaidHistoryDTO() {}

    public PaidHistoryDTO(String itemName, String category, BigDecimal finalPrice, LocalDateTime paidDate) {
        this.itemName = itemName;
        this.category = category;
        this.finalPrice = finalPrice;
        this.paidDate = paidDate;
    }

    public String getItemName() { return itemName; }
    public String getCategory() { return category; }
    public BigDecimal getFinalPrice() { return finalPrice; }
    public LocalDateTime getPaidDate() { return paidDate; }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public void setPaidDate(LocalDateTime paidDate) {
        this.paidDate = paidDate;
    }
}