package org.example.server.dao;

import org.example.core.models.items.AntiqueItem;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.JewelryItem;
import org.example.core.models.items.OtherItem;
import org.example.core.models.items.RealEstateItem;
import org.example.core.models.items.VehicleItem;

public class ItemDAO {
    private static ItemDAO instance = null;
    private ItemDAO() {}
    public ItemDAO getInstance() {
        if (instance == null) {
            synchronized (ItemDAO.class) {
                if (instance == null) {
                    instance = new ItemDAO();
                }
            }
        } return instance;
    }
}
class ItemFactory {
    public static Item createItem(String type) {
        return switch (type) {
            case "ArtItem" -> new ArtItem();
            case "ElectronicsItem" -> new ElectronicsItem();
            case "AntiqueItem" -> new AntiqueItem();
            case "JewelryItem" -> new JewelryItem();
            case "RealEstateItem" -> new RealEstateItem();
            case "VehicleItem" -> new VehicleItem();
            default -> new OtherItem();
        };
    }
}