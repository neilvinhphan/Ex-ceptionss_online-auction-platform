package org.example.server.validator;

import org.example.core.dto.CreateArtItemDTO;
import org.example.core.dto.CreateItemRequestDTO;

public class ArtItemValidator implements ItemValidatorStrategy{
    @Override
    public void validate(org.example.core.dto.itemsDTO.CreateItemRequestDTO dto) throws Exception {
        // Ép kiểu về DTO con
        org.example.core.dto.itemsDTO.CreateArtItemDTO artDto = (org.example.core.dto.itemsDTO.CreateArtItemDTO) dto;

        if (artDto.getArtist() == null || artDto.getArtist().trim().isEmpty()) {
            throw new Exception("Lỗi: Tác phẩm nghệ thuật phải có tên Tác giả!");
        }

        int currentYear = java.time.Year.now().getValue();
        if (artDto.getCreationYear() > currentYear) {
            throw new Exception("Lỗi: Năm sáng tác (" + artDto.getCreationYear() + ") không được lớn hơn năm hiện tại!");
        }
        if(artDto.getCreationYear() < 0) {
            throw new Exception("Lỗi: Năm sáng tác (" + artDto.getCreationYear() + ") không được nhỏ hơn 0!");
        }
    }
}
