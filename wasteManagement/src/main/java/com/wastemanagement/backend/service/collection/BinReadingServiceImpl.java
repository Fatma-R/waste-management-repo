package com.wastemanagement.backend.service.collection;

import com.wastemanagement.backend.dto.collection.BinReadingRequestDTO;
import com.wastemanagement.backend.dto.collection.BinReadingResponseDTO;
import com.wastemanagement.backend.mapper.collection.BinReadingMapper;
import com.wastemanagement.backend.model.collection.BinReading;
import com.wastemanagement.backend.repository.collection.BinReadingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BinReadingServiceImpl implements BinReadingService {

    private final BinReadingRepository repository;

    public BinReadingServiceImpl(BinReadingRepository repository) {
        this.repository = repository;
    }

    @Override
    public BinReadingResponseDTO create(BinReadingRequestDTO dto) {
        BinReading entity = BinReadingMapper.toEntity(dto);
        return BinReadingMapper.toResponseDTO(repository.save(entity));
    }

    @Override
    public List<BinReadingResponseDTO> getAll() {
        return repository.findAll().stream()
                .map(BinReadingMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BinReadingResponseDTO getById(String id) {
        BinReading entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("BinReading not found"));
        return BinReadingMapper.toResponseDTO(entity);
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }

    @Override
    public BinReadingResponseDTO findTopByBinIdOrderByTsDesc(String binId) {
        BinReading entity = repository.findTopByBinIdOrderByTsDesc(binId);
        if (entity == null) {
            throw new RuntimeException("BinReading not found");
        }
        return BinReadingMapper.toResponseDTO(entity);
    }
}
