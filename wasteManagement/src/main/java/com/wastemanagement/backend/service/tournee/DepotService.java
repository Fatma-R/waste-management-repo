package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.DepotRequestDTO;
import com.wastemanagement.backend.dto.tournee.DepotResponseDTO;
import com.wastemanagement.backend.model.tournee.Depot;

import java.util.List;
import java.util.Optional;

public interface DepotService {

    DepotResponseDTO createDepot(DepotRequestDTO requestDTO);

    DepotResponseDTO getDepotById(String id);

    List<DepotResponseDTO> getAllDepots();

    DepotResponseDTO updateDepot(String id, DepotRequestDTO requestDTO);

    void deleteDepot(String id);

    Optional<DepotResponseDTO> getMainDepot();

    DepotResponseDTO saveOrUpdateMainDepot(DepotRequestDTO requestDTO);

    Depot getMainDepotEntityOrThrow();
}
