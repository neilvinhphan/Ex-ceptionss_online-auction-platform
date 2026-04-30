package org.example.server.validator;

import org.example.core.dto.CreateElectronicsItemDTO;
import org.example.core.dto.CreateItemRequestDTO;

public class ElectronicsItemValidator implements ItemValidatorStrategy{
    @Override
    public void validate(CreateItemRequestDTO dto) throws Exception {
        CreateElectronicsItemDTO electronicsDto = (CreateElectronicsItemDTO) dto;

        if (electronicsDto.getBrand() == null || electronicsDto.getBrand().trim().isEmpty()) {
            throw new Exception("Lỗi: Đồ điện tử phải có tên Thương hiệu!");
        }

        if (electronicsDto.getWarrantyMonths() < 0) {
            throw new Exception("Lỗi: Thời gian bảo hành không được âm!");
        }

        if (electronicsDto.getCondition() == null || electronicsDto.getCondition().trim().isEmpty()) {
            throw new Exception("Lỗi: Đồ điện tử phải có tình trạng (condition)!");
        }
    }
}
