package org.example.core.dto.itemsDTO;

public class DeleteRequestDTO {
  private int itemId;

  public DeleteRequestDTO(int itemId) {
    this.itemId = itemId;
  }

  public int getItemId() {
    return itemId;
  }
}
