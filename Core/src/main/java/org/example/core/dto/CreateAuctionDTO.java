package org.example.core.dto;

import org.example.core.models.items.Item;

import java.util.List;

public class CreateAuctionDTO {
    private Item item;

    public CreateAuctionDTO(Item item) {
        this.item = item;
    }

    public void setItems(Item item) {
        this.item = item;
    }

    public Item getItems() {
        return item;
    }
}
