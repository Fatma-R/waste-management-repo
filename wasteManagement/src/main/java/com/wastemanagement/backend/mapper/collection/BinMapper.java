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

        return bin;
    }

    public static void merge(Bin existing, BinRequestDTO dto) {
        if (existing == null || dto == null) return;

        // Update non-primitives only if non-null
        if (dto.getCollectionPointId() != null) existing.setCollectionPointId(dto.getCollectionPointId());
        if (dto.getType() != null) existing.setType(dto.getType());


        // Always update primitives
        existing.setActive(dto.isActive());
    }

    public static BinResponseDTO toResponseDTO(Bin bin) {
        if (bin == null) return null;
        BinResponseDTO dto = new BinResponseDTO();
        dto.setId(bin.getId());
        dto.setCollectionPointId(bin.getCollectionPointId());
        dto.setActive(bin.isActive());
        dto.setType(bin.getType());

        return dto;
    }
}
