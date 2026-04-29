package org.example.server.validator;

import org.example.core.dto.CreateArtItemDTO;
import org.example.core.dto.CreateElectronicsItemDTO;
import org.example.core.dto.CreateItemRequestDTO;
import org.example.core.dto.CreateVehicleItemDTO;
import org.example.core.models.items.ArtItem;
import org.example.core.models.items.ElectronicsItem;
import org.example.core.models.items.Item;
import org.example.core.models.items.VehicleItem;

public class ItemCreationFactory {
    // Thêm tham số sellerId vào đây
    public static Item build(CreateItemRequestDTO dto, int sellerId) throws Exception {

        // 1. Nếu Client gửi lên Đồ nghệ thuật (Art)
        if (dto instanceof CreateArtItemDTO) {
            CreateArtItemDTO artDto = (CreateArtItemDTO) dto;

            // Truyền 3 tham số bắt buộc thẳng vào constructor
            return new ArtItem.Builder(sellerId, artDto.getItemName(), artDto.getStartingPrice())
                    // Chỉ gọi chaining cho các tham số optional (thuộc tính riêng)
                    .artist(artDto.getArtist())
                    .creationYear(artDto.getCreationYear())
                    .build();
        }

        // 2. Nếu Client gửi lên Xe cộ (Vehicle)
        else if (dto instanceof CreateVehicleItemDTO) {
            CreateVehicleItemDTO vehicleDto = (CreateVehicleItemDTO) dto;

            return new VehicleItem.Builder(sellerId, vehicleDto.getItemName(), vehicleDto.getStartingPrice())
                    .brand(vehicleDto.getBrand())
                    .mileage(vehicleDto.getMileage())
                    .build();
        }

        // 3. Nếu Client gửi lên Đồ điện tử (Electronics)
        else if (dto instanceof CreateElectronicsItemDTO) {
            CreateElectronicsItemDTO electronicsDto = (CreateElectronicsItemDTO) dto;

            return new ElectronicsItem.Builder(sellerId, electronicsDto.getItemName(), electronicsDto.getStartingPrice())
                    .brand(electronicsDto.getBrand())
                    .warrantyMonths(electronicsDto.getWarrantyMonths())
                    .condition(electronicsDto.getCondition())
                    .build();
        }

        throw new Exception("Factory chưa hỗ trợ khởi tạo cho loại danh mục này!");
    }
}