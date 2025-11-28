package com.wastemanagement.backend.service.collection;

import com.wastemanagement.backend.dto.collection.BinRequestDTO;
import com.wastemanagement.backend.dto.collection.BinResponseDTO;
import com.wastemanagement.backend.mapper.collection.BinMapper;
import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.model.collection.CollectionPoint;
import com.wastemanagement.backend.repository.CollectionPointRepository;
import com.wastemanagement.backend.repository.collection.BinRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BinServiceImpl implements BinService {

    private final BinRepository binRepository;
    private final CollectionPointRepository collectionPointRepository;

    public BinServiceImpl(BinRepository binRepository,
                          CollectionPointRepository collectionPointRepository) {
        this.binRepository = binRepository;
        this.collectionPointRepository = collectionPointRepository;
    }

    @Override
    public List<BinResponseDTO> getAllBins() {
        return binRepository.findAll()
                .stream()
                .map(BinMapper::toResponseDTO)
                .toList();
    }

    @Override
    public Optional<BinResponseDTO> getBinById(String id) {
        return binRepository.findById(id)
                .map(BinMapper::toResponseDTO);
    }

    @Override
    public BinResponseDTO createBin(BinRequestDTO dto) {

        // 1. Vérifier si le CollectionPoint existe
        CollectionPoint cp = collectionPointRepository.findById(dto.getCollectionPointId())
                .orElseThrow(() -> new RuntimeException("CollectionPoint not found"));

        // 2. Convertir DTO -> Entity
        Bin bin = BinMapper.toEntity(dto);

        // 3. Sauvegarder le Bin
        Bin saved = binRepository.save(bin);

        // 4. Ajouter le bin au collection point
        if (cp.getBins() == null) {
            cp.setBins(new ArrayList<>());
        }
        cp.getBins().add(saved);

        // 5. Sauvegarder le collection point
        collectionPointRepository.save(cp);

        // 6. Retourner le DTO
        return BinMapper.toResponseDTO(saved);
    }

    @Override
    public Optional<BinResponseDTO> updateBin(String id, BinRequestDTO dto) {
        return binRepository.findById(id)
                .map(existing -> {
                    BinMapper.merge(existing, dto);
                    Bin updated = binRepository.save(existing);
                    return BinMapper.toResponseDTO(updated);
                });
    }

    @Override
    public boolean deleteBin(String id) {
        return binRepository.findById(id)
                .map(bin -> {
                    binRepository.delete(bin);
                    return true;
                })
                .orElse(false);
    }
}
