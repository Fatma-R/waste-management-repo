package com.wastemanagement.backend.mapper.collection;
import com.wastemanagement.backend.dto.collection.BinRequestDTO;
import com.wastemanagement.backend.dto.collection.BinResponseDTO;
import com.wastemanagement.backend.model.collection.Bin;

public class BinMapper {

    public static Bin toEntity(BinRequestDTO dto) {
        if (dto == null) return null;
        Bin bin = new Bin();
        bin.setCollectionPointId(dto.getCollectionPointId());
        bin.setActive(dto.isActive());
        bin.setType(dto.getType());
        bin.setReadingIds(dto.getReadingIds());
        bin.setAlertIds(dto.getAlertIds());
        return bin;
    }

    public static void merge(Bin existing, BinRequestDTO dto) {
        existing.setCollectionPointId(dto.getCollectionPointId());
        existing.setActive(dto.isActive());
        existing.setType(dto.getType());
        existing.setReadingIds(dto.getReadingIds());
        existing.setAlertIds(dto.getAlertIds());
    }

    public static BinResponseDTO toResponseDTO(Bin bin) {
        if (bin == null) return null;
        BinResponseDTO dto = new BinResponseDTO();
        dto.setId(bin.getId());
        dto.setCollectionPointId(bin.getCollectionPointId());
        dto.setActive(bin.isActive());
        dto.setType(bin.getType());
        dto.setReadingIds(bin.getReadingIds());
        dto.setAlertIds(bin.getAlertIds());
        return dto;
    }
}
