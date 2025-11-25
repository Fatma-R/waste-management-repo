package com.wastemanagement.backend.service.collectionPoint;


import com.wastemanagement.backend.dto.collectionPoint.CollectionPointRequestDTO;
import com.wastemanagement.backend.dto.collectionPoint.CollectionPointResponseDTO;

import java.util.List;

public interface CollectionPointService {
    CollectionPointResponseDTO create(CollectionPointRequestDTO dto);
    CollectionPointResponseDTO getById(String id);
    List<CollectionPointResponseDTO> getAll();
    CollectionPointResponseDTO update(String id, CollectionPointRequestDTO dto);
    void delete(String id);
}
