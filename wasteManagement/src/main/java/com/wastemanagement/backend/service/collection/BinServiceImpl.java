package com.wastemanagement.backend.service.collection;

import com.wastemanagement.backend.dto.collection.BinRequestDTO;
import com.wastemanagement.backend.dto.collection.BinResponseDTO;
import com.wastemanagement.backend.mapper.collection.BinMapper;
import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.repository.collection.BinRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BinServiceImpl implements BinService {

    private final BinRepository binRepository;

    public BinServiceImpl(BinRepository binRepository) {
        this.binRepository = binRepository;
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
        Bin entity = BinMapper.toEntity(dto);
        Bin saved = binRepository.save(entity);
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
