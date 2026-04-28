package org.example.core.models.items;

import java.sql.ResultSet;

public class ItemFactory {
  public static Item takeItemFromDB(ResultSet rs) throws Exception {
    String type = rs.getString("type");
    Item item;
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
      case "Antique" -> {
        AntiqueItem antiqueItem = new AntiqueItem();
        antiqueItem.setEra(rs.getString("era"));
        antiqueItem.setMaterial(rs.getString("material"));
        antiqueItem.setCondition(rs.getString("item_condition"));
        antiqueItem.setCertified(rs.getBoolean("is_certified"));
        item = antiqueItem;
      }
      case "Jewelry" -> {
        JewelryItem jewelryItem = new JewelryItem();
        jewelryItem.setCertification(rs.getString("certification"));
        jewelryItem.setGemstone(rs.getString("gemstone"));
        jewelryItem.setMaterial(rs.getString("material"));
        jewelryItem.setWeight(rs.getDouble("weight"));
        item = jewelryItem;
      }
      case "RealEstate" -> {
        RealEstateItem realEstateItem = new RealEstateItem();
        realEstateItem.setLocation(rs.getString("location"));
        realEstateItem.setArea(rs.getDouble("area"));
        realEstateItem.setPropertyType(rs.getString("property_type"));
        realEstateItem.setLegalStatus(rs.getString("legal_status"));
        item = realEstateItem;
      }
      case "Vehicle" -> {
        VehicleItem vehicleItem = new VehicleItem();
        vehicleItem.setBrand(rs.getString("brand"));
        vehicleItem.setModel(rs.getString("model"));
        vehicleItem.setManufacturingYear(rs.getInt("manufacturing_year"));
        vehicleItem.setMileage(rs.getInt("mileage"));
        item = vehicleItem;
      }
      default -> item = new OtherItem();
    }
    return item;
  }
}
