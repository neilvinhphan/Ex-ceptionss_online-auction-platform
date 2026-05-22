package org.example.server.validator;

import org.example.core.dto.itemsDTO.CreateItemRequestDTO;

import java.util.HashMap;
import java.util.Map;

public class ItemValidatorFactory {
    // Map chứa các chiến lược kiểm duyệt
    private static final Map<String, ItemValidatorStrategy> validators = new HashMap<>();

    static {
        validators.put("ART", new ArtItemValidator());
        validators.put("VEHICLE", new VehicleItemValidator());
        validators.put("ELECTRONICS", new ElectronicsItemValidator());
        // Thêm các danh mục khác vào đây...
    }

    public static void validateSpecifics(org.example.core.dto.itemsDTO.CreateItemRequestDTO dto) throws Exception {
        ItemValidatorStrategy validator = validators.get(dto.getType().toUpperCase());
        if (validator != null) {
            validator.validate(dto); // Tự động chạy đúng hàm validate của class con
        } else {
            throw new Exception("Hệ thống chưa hỗ trợ kiểm duyệt danh mục: " + dto.getType());
        }
    }
}
