package org.example.core.models.items;

import org.example.core.dto.CreateArtItemDTO;
import org.example.core.dto.CreateElectronicsItemDTO;
import org.example.core.dto.CreateItemRequestDTO;
import org.example.core.dto.CreateVehicleItemDTO;

import java.sql.ResultSet;

public class ItemFactory {
  public static Item takeItemFromDB(ResultSet rs) throws Exception {
    String type = rs.getString("type");
    Item item;
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
    // Map các trường chung từ DTO sang Model
    trueItem.setType(type);
    trueItem.setItemName(itemDTO.getItemName());
    trueItem.setDescription(itemDTO.getDescription());
    trueItem.setSellerID(itemDTO.getSellerID());
    trueItem.setStartingPrice(itemDTO.getStartingPrice());
    trueItem.setImage(itemDTO.getBase64Image());

    return trueItem;
  }
}
