package com.wastemanagement.backend.service.collection;

import com.wastemanagement.backend.dto.collection.BinRequestDTO;
import com.wastemanagement.backend.dto.collection.BinResponseDTO;
import com.wastemanagement.backend.mapper.collection.BinMapper;
import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.model.collection.CollectionPoint;
import com.wastemanagement.backend.repository.CollectionPointRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BinServiceImpl implements BinService {

    private final CollectionPointRepository collectionPointRepository;

    public BinServiceImpl(CollectionPointRepository collectionPointRepository) {
        this.collectionPointRepository = collectionPointRepository;
    }

    @Override
    public List<BinResponseDTO> getAllBins() {
        return collectionPointRepository.findAll()
                .stream()
                .filter(cp -> cp.getBins() != null)
                .flatMap(cp -> cp.getBins().stream())
                .map(BinMapper::toResponseDTO)
                .toList();
    }

    @Override
    public Optional<BinResponseDTO> getBinById(String id) {
        return collectionPointRepository.findAll()
                .stream()
                .filter(cp -> cp.getBins() != null)
                .flatMap(cp -> cp.getBins().stream())
                .filter(b -> id.equals(b.getId()))
                .findFirst()
                .map(BinMapper::toResponseDTO);
    }

    @Override
    public BinResponseDTO createBin(BinRequestDTO dto) {

        // 1. Check collection point
        CollectionPoint cp = collectionPointRepository.findById(dto.getCollectionPointId())
                .orElseThrow(() -> new RuntimeException("CollectionPoint not found"));

        // 2. DTO -> entity
        Bin bin = BinMapper.toEntity(dto);

        // Ensure we have an ID for the embedded bin
        if (bin.getId() == null || bin.getId().isBlank()) {
            bin.setId(new ObjectId().toHexString()); // or UUID.randomUUID().toString()
        }

        // Make sure collectionPointId is consistent
        bin.setCollectionPointId(cp.getId());

        // 3. Add to embedded list
        if (cp.getBins() == null) {
            cp.setBins(new ArrayList<>());
        }
        cp.getBins().add(bin);

        // 4. Save CP (this persists the embedded bin)
        collectionPointRepository.save(cp);

        // 5. Return DTO
        return BinMapper.toResponseDTO(bin);
    }
    @Override
    public Optional<BinResponseDTO> updateBin(String id, BinRequestDTO dto) {

        List<CollectionPoint> allCps = collectionPointRepository.findAll();

        for (CollectionPoint cp : allCps) {
            if (cp.getBins() == null) continue;

            for (Bin bin : cp.getBins()) {
                if (id.equals(bin.getId())) {
                    // Mettre à jour le bin embarqué
                    BinMapper.merge(bin, dto);
                    collectionPointRepository.save(cp);
                    return Optional.of(BinMapper.toResponseDTO(bin));
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean deleteBin(String id) {

        List<CollectionPoint> allCps = collectionPointRepository.findAll();

        for (CollectionPoint cp : allCps) {
            if (cp.getBins() == null) continue;

            boolean removed = cp.getBins().removeIf(b -> id.equals(b.getId()));
            if (removed) {
                collectionPointRepository.save(cp);
                return true;
            }
        }

        return false;
    }
}
