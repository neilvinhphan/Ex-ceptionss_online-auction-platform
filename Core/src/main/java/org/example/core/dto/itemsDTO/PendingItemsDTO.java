package org.example.core.dto.itemsDTO;

import org.example.core.models.items.Item;

import java.util.List;

public class PendingItemsDTO {
    private List<Item> items;
    private int sellerId;

    public PendingItemsDTO(int sellerId) {this.sellerId = sellerId;}

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setSellerId(int sellerId) {this.sellerId = sellerId;}

    public int getSellerId() {return sellerId;}

}
