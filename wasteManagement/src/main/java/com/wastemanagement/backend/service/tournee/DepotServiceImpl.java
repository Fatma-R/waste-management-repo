package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.DepotRequestDTO;
import com.wastemanagement.backend.dto.tournee.DepotResponseDTO;
import com.wastemanagement.backend.mapper.tournee.DepotMapper;
import com.wastemanagement.backend.model.tournee.Depot;
import com.wastemanagement.backend.repository.tournee.DepotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class DepotServiceImpl implements DepotService {

    public static final String MAIN_DEPOT_ID = "MAIN_DEPOT";

    private final DepotRepository depotRepository;
    private final DepotMapper depotMapper;

    public DepotServiceImpl(DepotRepository depotRepository, DepotMapper depotMapper) {
        this.depotRepository = depotRepository;
        this.depotMapper = depotMapper;
    }

    // CRUD standard (multi-depots)

    @Override
    public DepotResponseDTO createDepot(DepotRequestDTO requestDTO) {
        Depot depot = depotMapper.toEntity(requestDTO);
        // on s’assure de ne pas écraser un id existant lors de la création
        depot.setId(null);
        Depot saved = depotRepository.save(depot);
        return depotMapper.toResponseDTO(saved);
    }

    @Override
    public DepotResponseDTO getDepotById(String id) {
        Optional<Depot> optionalDepot = depotRepository.findById(id);
        return optionalDepot.map(depotMapper::toResponseDTO).orElse(null);
    }

    @Override
    public List<DepotResponseDTO> getAllDepots() {
        return StreamSupport.stream(depotRepository.findAll().spliterator(), false)
                .map(depotMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DepotResponseDTO updateDepot(String id, DepotRequestDTO requestDTO) {
        Optional<Depot> optionalDepot = depotRepository.findById(id);
        if (optionalDepot.isEmpty()) {
            return null;
        }

        Depot existingDepot = optionalDepot.get();
        depotMapper.updateEntityFromRequest(requestDTO, existingDepot);
        Depot updated = depotRepository.save(existingDepot);
        return depotMapper.toResponseDTO(updated);
    }

    @Override
    public void deleteDepot(String id) {
        depotRepository.deleteById(id);
    }

    // MAIN DEPOT helpers (singleton "MAIN_DEPOT")

    public Optional<DepotResponseDTO> getMainDepot() {
        return depotRepository.findById(MAIN_DEPOT_ID)
                .map(depotMapper::toResponseDTO);
    }

    public DepotResponseDTO saveOrUpdateMainDepot(DepotRequestDTO requestDTO) {
        // Soit on récupère l’existant, soit on crée un nouvel objet
        Depot depot = depotRepository.findById(MAIN_DEPOT_ID)
                .orElse(new Depot());

        depot.setId(MAIN_DEPOT_ID);
        // On met à jour les champs depuis le DTO de requête
        depotMapper.updateEntityFromRequest(requestDTO, depot);

        Depot saved = depotRepository.save(depot);
        return depotMapper.toResponseDTO(saved);
    }

    /**
     * Version "domain" pour le backend (VROOM, etc.),
     * sans passer par les DTO.
     */
    public Depot getMainDepotEntityOrThrow() {
        return depotRepository.findById(MAIN_DEPOT_ID)
                .orElseThrow(() -> new IllegalStateException("Main depot not configured"));
    }
}
