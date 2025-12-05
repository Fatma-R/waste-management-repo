package com.wastemanagement.backend.service.collection;

import com.wastemanagement.backend.dto.collection.BinReadingRequestDTO;
import com.wastemanagement.backend.dto.collection.BinReadingResponseDTO;
import java.util.List;

public interface BinReadingService {
    BinReadingResponseDTO create(BinReadingRequestDTO dto);
    List<BinReadingResponseDTO> getAll();
    BinReadingResponseDTO getById(String id);
    void delete(String id);
    BinReadingResponseDTO findTopByBinIdOrderByTsDesc(String binId);
}
