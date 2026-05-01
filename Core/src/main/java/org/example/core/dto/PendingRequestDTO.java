package org.example.core.dto;

import org.example.core.models.items.Item;

import java.util.List;

public class PendingRequestDTO {
    private List<Item> items;
    private int sellerId;

    public PendingRequestDTO(int sellerId) {this.sellerId = sellerId;}

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setSellerId(int sellerId) {this.sellerId = sellerId;}

    public int getSellerId() {return sellerId;}

}
