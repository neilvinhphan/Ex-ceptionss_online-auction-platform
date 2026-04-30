
package org.example.server.validator;

import org.example.core.dto.CreateElectronicsItemDTO;
import org.example.core.dto.CreateItemRequestDTO;
import org.example.core.dto.CreateVehicleItemDTO;

public class ElectronicsItemValidator implements ItemValidatorStrategy {
    @Override
    public void validate(CreateItemRequestDTO dto) throws Exception {
        // Ep kieu ve DTO con
        CreateElectronicsItemDTO electronicsDto = (CreateElectronicsItemDTO) dto;

        if (electronicsDto.getBrand() == null || electronicsDto.getBrand().trim().isEmpty()) {
            throw new Exception("Lỗi: Đồ điện tử phải có thương hiệu!");
        }

        if (electronicsDto.getCondition() == null || electronicsDto.getCondition().trim().isEmpty()) {
            throw new Exception("Lỗi: Đồ điện tử phải có tình trạng sản phẩm!");
        }

        if (electronicsDto.getWarrantyMonths() < 0) {
            throw new Exception("Lỗi: Số tháng bảo hành (" + electronicsDto.getWarrantyMonths() + ") không được nhỏ hơn 0!");
        }
    }
}
