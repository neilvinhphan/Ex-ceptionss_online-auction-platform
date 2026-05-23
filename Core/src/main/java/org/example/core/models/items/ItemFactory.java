package org.example.core.models.items;

import org.example.core.dto.itemsDTO.CreateArtItemDTO;
import org.example.core.dto.itemsDTO.CreateElectronicsItemDTO;
import org.example.core.dto.itemsDTO.CreateItemRequestDTO;
import org.example.core.dto.itemsDTO.CreateVehicleItemDTO;
import org.example.core.shared.enums.ItemStatus;

import java.sql.ResultSet;

public class ItemFactory {
  public static Item takeItemFromDB(ResultSet rs) throws Exception {
    String type = rs.getString("type");
    Item item = null;
    switch (type) {
      case "ART" -> {
        ArtItem artItem = new ArtItem();
        artItem.setType("ART");
        artItem.setArtist(rs.getString("artist"));
        artItem.setCreationYear(rs.getInt("creation_year"));
        item = artItem;
      }
      case "ELECTRONICS" -> {
        ElectronicsItem electronicsItem = new ElectronicsItem();
        electronicsItem.setType("ELECTRONICS");
        electronicsItem.setBrand(rs.getString("ele_brand"));
        electronicsItem.setWarrantyMonths(rs.getInt("warranty_months"));
        electronicsItem.setCondition(rs.getString("item_condition"));
        item = electronicsItem;
      }

      case "VEHICLE" -> {
        VehicleItem vehicleItem = new VehicleItem();
        vehicleItem.setType("VEHICLE");
        vehicleItem.setBrand(rs.getString("veh_brand"));
        vehicleItem.setModel(rs.getString("model"));
        vehicleItem.setManufacturingYear(rs.getInt("manufacturing_year"));
        vehicleItem.setMileage(rs.getInt("mileage"));
        item = vehicleItem;
      }
      default -> throw new Exception("Unknown item type: " + type);
    }

    if (item != null) { // Set các thông tin chung
      item.setItemId(rs.getInt("items_id"));
      item.setItemName(rs.getString("items_name"));
      item.setDescription(rs.getString("description"));
      item.setStartingPrice(rs.getBigDecimal("start_price"));
      item.setType(type);
      item.setImage(rs.getString("image"));
      item.setStatus(ItemStatus.valueOf(rs.getString("status")));

      // --- BỔ SUNG 2 DÒNG NÀY ĐỂ LẤY DỮ LIỆU AI ---
      item.setSuggestedPrice(rs.getBigDecimal("suggested_price"));
      item.setAiReason(rs.getString("ai_reason"));
    }

    return item;
  }

  public static Item createItemDTO(CreateItemRequestDTO itemDTO) throws Exception {
    Item trueItem;
    String type = itemDTO.getType().toUpperCase();

    switch (type) {
      case "ART" -> {
        CreateArtItemDTO artItemDTO = (CreateArtItemDTO) itemDTO;
        ArtItem artItem = new ArtItem();
        artItem.setArtist((artItemDTO.getArtist()));
        artItem.setCreationYear(artItemDTO.getCreationYear());
        trueItem = artItem;
      }
      case "ELECTRONICS" -> {
        CreateElectronicsItemDTO electronicsItemDTO = (CreateElectronicsItemDTO) itemDTO;
        ElectronicsItem electronicsItem = new ElectronicsItem();
        electronicsItem.setBrand((electronicsItemDTO.getBrand()));
        electronicsItem.setWarrantyMonths(electronicsItem.getWarrantyMonths());
        electronicsItem.setCondition(electronicsItem.getCondition());
        trueItem = electronicsItem;
      }
      case "VEHICLE" -> {
        CreateVehicleItemDTO vehicleItemDTO = (CreateVehicleItemDTO) itemDTO;
        VehicleItem vehicleItem = new VehicleItem();
        vehicleItem.setBrand(vehicleItemDTO.getBrand());
        vehicleItem.setModel(vehicleItemDTO.getModel());
        vehicleItem.setManufacturingYear(vehicleItemDTO.getManufacturingYear());
        vehicleItem.setMileage(vehicleItemDTO.getMileage());
        trueItem = vehicleItem;
      }
      default -> throw new Exception("Unknown item type: " + itemDTO.getType());
    }

    trueItem.setType(type);
    trueItem.setItemName(itemDTO.getItemName());
    trueItem.setDescription(itemDTO.getDescription());
    trueItem.setSellerID(itemDTO.getSellerID());
    trueItem.setStartingPrice(itemDTO.getStartingPrice());
    trueItem.setImage(itemDTO.getBase64Image());

    trueItem.setStatus(ItemStatus.PENDING);

    return trueItem;
  }
}
