package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.mapper.tournee.TourneeMapper;
import com.wastemanagement.backend.model.tournee.Tournee;
import com.wastemanagement.backend.repository.tournee.TourneeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourneeServiceImpl implements TourneeService {

    private final TourneeRepository tourneeRepository;

    @Override
    public TourneeResponseDTO createTournee(TourneeRequestDTO dto) {
        Tournee tournee = TourneeMapper.toEntity(dto);
        Tournee saved = tourneeRepository.save(tournee);
        return TourneeMapper.toResponse(saved);
    }

    @Override
    public TourneeResponseDTO updateTournee(String id, TourneeRequestDTO dto) {
        Tournee existing = tourneeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournee not found"));
        TourneeMapper.updateEntity(existing, dto);
        Tournee saved = tourneeRepository.save(existing);
        return TourneeMapper.toResponse(saved);
    }

    @Override
    public TourneeResponseDTO getTourneeById(String id) {
        Tournee tournee = tourneeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournee not found"));
        return TourneeMapper.toResponse(tournee);
    }

    @Override
    public List<TourneeResponseDTO> getAllTournees() {
        return ((List<Tournee>) tourneeRepository.findAll())
                .stream()
                .map(TourneeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteTournee(String id) {
        tourneeRepository.deleteById(id);
    }
}
