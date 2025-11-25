package com.wastemanagement.backend.mapper;


import com.wastemanagement.backend.dto.collectionPoint.CollectionPointRequestDTO;
import com.wastemanagement.backend.dto.collectionPoint.CollectionPointResponseDTO;
import com.wastemanagement.backend.model.collection.CollectionPoint;
import org.springframework.stereotype.Component;

@Component
public class CollectionPointMapper {

    public CollectionPointResponseDTO toResponseDTO(CollectionPoint cp) {
        if (cp == null) return null;
        return new CollectionPointResponseDTO(
                cp.getId(),
                cp.getLocation(),
                cp.isActive(),
                cp.getAdresse(),
                cp.getBins()
        );
    }

    public CollectionPoint toEntity(CollectionPointRequestDTO dto) {
        if (dto == null) return null;
        return new CollectionPoint(
                null,
                dto.getLocation(),
                dto.isActive(),
                dto.getAdresse(),
                null
        );
    }

    public void updateEntity(CollectionPointRequestDTO dto, CollectionPoint entity) {
        if (dto.getLocation() != null) entity.setLocation(dto.getLocation());
        entity.setActive(dto.isActive());
        if (dto.getAdresse() != null) entity.setAdresse(dto.getAdresse());

    }
}
