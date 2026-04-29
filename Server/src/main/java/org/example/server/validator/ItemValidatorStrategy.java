package org.example.server.validator;

import org.example.core.dto.CreateItemRequestDTO;

public interface ItemValidatorStrategy {
    // Nhận DTO cha vào, nhưng bên trong sẽ ép kiểu về DTO con để xử lý
    void validate(CreateItemRequestDTO dto) throws Exception;
}
