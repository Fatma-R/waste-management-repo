package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeAssignmentRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeAssignmentResponseDTO;
import com.wastemanagement.backend.mapper.tournee.TourneeAssignmentMapper;
import com.wastemanagement.backend.model.tournee.TourneeAssignment;
import com.wastemanagement.backend.repository.tournee.TourneeAssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TourneeAssignmentServiceImpl implements TourneeAssignmentService {

    private final TourneeAssignmentRepository repo;

    public TourneeAssignmentServiceImpl(TourneeAssignmentRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<TourneeAssignmentResponseDTO> getAll() {
        return repo.findAll().stream()
                .map(TourneeAssignmentMapper::toResponseDTO)
                .toList();
    }

    @Override
    public Optional<TourneeAssignmentResponseDTO> getById(String id) {
        return repo.findById(id)
                .map(TourneeAssignmentMapper::toResponseDTO);
    }

    @Override
    public TourneeAssignmentResponseDTO create(TourneeAssignmentRequestDTO dto) {
        TourneeAssignment entity = TourneeAssignmentMapper.toEntity(dto);
        repo.save(entity);
        return TourneeAssignmentMapper.toResponseDTO(entity);
    }

    @Override
    public Optional<TourneeAssignmentResponseDTO> update(String id, TourneeAssignmentRequestDTO dto) {
        return repo.findById(id)
                .map(existing -> {
                    TourneeAssignmentMapper.merge(existing, dto);
                    repo.save(existing);
                    return TourneeAssignmentMapper.toResponseDTO(existing);
                });
    }

    @Override
    public boolean delete(String id) {
        return repo.findById(id)
                .map(a -> {
                    repo.delete(a);
                    return true;
                })
                .orElse(false);
    }
}
