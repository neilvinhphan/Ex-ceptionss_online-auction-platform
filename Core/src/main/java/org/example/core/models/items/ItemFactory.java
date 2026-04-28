package org.example.core.models.items;

import java.sql.ResultSet;

public class ItemFactory {
  public static Item takeItemFromDB(ResultSet rs) throws Exception {
    String type = rs.getString("type");
    Item item = null;
    switch (type) {
      case "Art" -> {
        ArtItem artItem = new ArtItem();
        artItem.setArtist(rs.getString("artist"));
        artItem.setCreationYear(rs.getInt("creation_year"));
        item = artItem;
      }
      case "Electronics" -> {
        ElectronicsItem electronicsItem = new ElectronicsItem();
        electronicsItem.setBrand(rs.getString("brand"));
        electronicsItem.setWarrantyMonths(rs.getInt("warranty_months"));
        electronicsItem.setCondition(rs.getString("condition"));
        item = electronicsItem;
      }

      case "Vehicle" -> {
        VehicleItem vehicleItem = new VehicleItem();
        vehicleItem.setBrand(rs.getString("brand"));
        vehicleItem.setModel(rs.getString("model"));
        vehicleItem.setManufacturingYear(rs.getInt("manufacturing_year"));
        vehicleItem.setMileage(rs.getInt("mileage"));
        item = vehicleItem;
      }
    }
    return item;
  }
}
