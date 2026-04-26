package org.example.server.services;

import org.example.core.dto.AuctionRequestDTO;
import org.example.core.models.entities.Auction;
import org.example.core.models.items.Item;
import org.example.core.models.items.OtherItem;
import org.example.core.shared.enums.AuctionStatus;
import org.example.core.shared.enums.ItemStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionServiceTest {

  @Test
  void createAuction_throwsWhenItemIsNull() {
    AuctionRequestDTO request = new AuctionRequestDTO(null, 10);

    Exception ex =
        assertThrows(Exception.class, () -> AuctionService.createAuction(request));

    assertEquals("Vật phẩm không tồn tại!", ex.getMessage());
  }

  @Test
  void createAuction_throwsWhenItemIsListed() {
    Item listedItem = buildItem(ItemStatus.LISTED);
    AuctionRequestDTO request = new AuctionRequestDTO(listedItem, 15);

    Exception ex =
        assertThrows(Exception.class, () -> AuctionService.createAuction(request));

    assertEquals("Vật phẩm đang được đấu giá!", ex.getMessage());
  }

  @Test
  void createAuction_createsWarehouseAuctionForDraftItem() throws Exception {
    Item draftItem = buildItem(ItemStatus.DRAFT);
    AuctionRequestDTO request = new AuctionRequestDTO(draftItem, 30);

    Auction auction = AuctionService.createAuction(request);

    assertNotNull(auction);
    assertSame(draftItem, auction.getItem());
    assertEquals(30, auction.getDurationMinutes());
    assertEquals(AuctionStatus.WAREHOUSE, auction.getStatus());
    assertNotNull(auction.getStartTime());
  }

  private static Item buildItem(ItemStatus status) {
    OtherItem item =
        new OtherItem.Builder(1, "Laptop cu", BigDecimal.valueOf(1_000_000))
            .category("Electronics")
            .origin("VN")
            .weight(1.5)
            .build();
    item.setStatus(status);
    return item;
  }
}
