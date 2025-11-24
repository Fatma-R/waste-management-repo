package com.wastemanagement.backend.service.collection;

import com.wastemanagement.backend.dto.collection.BinRequestDTO;
import com.wastemanagement.backend.dto.collection.BinResponseDTO;

import java.util.List;
import java.util.Optional;

public interface BinService {

    List<BinResponseDTO> getAllBins();

    Optional<BinResponseDTO> getBinById(String id);

    BinResponseDTO createBin(BinRequestDTO dto);

    Optional<BinResponseDTO> updateBin(String id, BinRequestDTO dto);

    boolean deleteBin(String id);
}
