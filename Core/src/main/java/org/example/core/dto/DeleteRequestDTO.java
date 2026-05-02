package org.example.core.dto;

public class DeleteRequestDTO {
    private int itemId;
    public DeleteRequestDTO(int itemId) {
        this.itemId = itemId;
    }
    public int getItemId() {
        return itemId;
    }
    public void setItemId(int itemId) {this.itemId = itemId;}
}
