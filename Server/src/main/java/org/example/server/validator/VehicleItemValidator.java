package org.example.server.validator;

import org.example.core.dto.CreateElectronicsItemDTO;
import org.example.core.dto.CreateItemRequestDTO;
import org.example.core.dto.CreateVehicleItemDTO;

public class VehicleItemValidator implements ItemValidatorStrategy {
    @Override
    public void validate(CreateItemRequestDTO dto) throws Exception {
        // Ep kieu ve DTO con
        CreateVehicleItemDTO vehicleDto = (CreateVehicleItemDTO) dto;

        if (vehicleDto.getBrand() == null || vehicleDto.getBrand().trim().isEmpty()) {
            throw new Exception("Lỗi: Xe phải có hãng xe!");
        }

        if (vehicleDto.getModel() == null || vehicleDto.getModel().trim().isEmpty()) {
            throw new Exception("Lỗi: Xe phải có model!");
        }

        int currentYear = java.time.Year.now().getValue();
        if (vehicleDto.getManufacturingYear() > currentYear) {
            throw new Exception("Lỗi: Năm sản xuất (" + vehicleDto.getManufacturingYear() + ") không được lớn hơn năm hiện tại!");
        }
        if (vehicleDto.getManufacturingYear() < 0) {
            throw new Exception("Lỗi: Năm sản xuất (" + vehicleDto.getManufacturingYear() + ") không được nhỏ hơn 0!");
        }

        if (vehicleDto.getMileage() < 0) {
            throw new Exception("Lỗi: Số km đã đi (" + vehicleDto.getMileage() + ") không được nhỏ hơn 0!");
        }
    }
}






