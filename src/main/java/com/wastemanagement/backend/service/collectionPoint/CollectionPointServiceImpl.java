package com.wastemanagement.backend.service.collectionPoint;


import com.wastemanagement.backend.dto.collectionPoint.CollectionPointRequestDTO;
import com.wastemanagement.backend.dto.collectionPoint.CollectionPointResponseDTO;
import com.wastemanagement.backend.mapper.CollectionPointMapper;
import com.wastemanagement.backend.model.collection.CollectionPoint;
import com.wastemanagement.backend.repository.CollectionPointRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CollectionPointServiceImpl implements CollectionPointService {

    private final CollectionPointRepository repository;
    private final CollectionPointMapper mapper;

    public CollectionPointServiceImpl(CollectionPointRepository repository, CollectionPointMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public CollectionPointResponseDTO create(CollectionPointRequestDTO dto) {
        CollectionPoint entity = mapper.toEntity(dto);
        repository.save(entity);
        return mapper.toResponseDTO(entity);
    }

    @Override
    public CollectionPointResponseDTO getById(String id) {
        return repository.findById(id)
                .map(mapper::toResponseDTO)
                .orElse(null);
    }

    @Override
    public List<CollectionPointResponseDTO> getAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CollectionPointResponseDTO update(String id, CollectionPointRequestDTO dto) {
        return repository.findById(id).map(entity -> {
            mapper.updateEntity(dto, entity);
            repository.save(entity);
            return mapper.toResponseDTO(entity);
        }).orElse(null);
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }
}

