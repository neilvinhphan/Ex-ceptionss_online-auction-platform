package org.example.core.dto;

import java.math.BigDecimal;

public class CreateElectronicsItemDTO extends CreateItemRequestDTO{
    private String brand;
    private int warrantyMonths;
    private String condition;

    public CreateElectronicsItemDTO(){
    }


    public String getBrand() {return brand;}

    public void setBrand(String brand) {this.brand = brand;}

    public int getWarrantyMonths() {return warrantyMonths;}

    public void setWarrantyMonths(int warrantyMonths) {this.warrantyMonths = warrantyMonths;}

    public String getCondition() {return condition;}

    public void setCondition(String condition) {this.condition = condition;}
}
