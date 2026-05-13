package org.example.server.validator;

import org.example.core.dto.CreateItemRequestDTO;
import org.example.core.dto.CreateVehicleItemDTO;

public class VehicleItemValidator implements ItemValidatorStrategy{
    @Override
    public void validate(org.example.core.dto.itemsDTO.CreateItemRequestDTO dto) {
        org.example.core.dto.itemsDTO.CreateVehicleItemDTO vehicleDto = (org.example.core.dto.itemsDTO.CreateVehicleItemDTO) dto;

        if (vehicleDto.getBrand() == null || vehicleDto.getBrand().trim().isEmpty()) {
            throw new IllegalArgumentException("Lỗi: Xe phải có tên Hãng sản xuất (Make)!");
        }

        if (vehicleDto.getModel() == null || vehicleDto.getModel().trim().isEmpty()) {
            throw new IllegalArgumentException("Lỗi: Xe phải có tên Mẫu xe (Model)!");
        }

        if (vehicleDto.getMileage() < 0) {
            throw new IllegalArgumentException("Lỗi: Số km đã đi không được âm!");
        }
    }
}
