package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.mapper.employee.AdminMapper;
import com.wastemanagement.backend.mapper.tournee.TourneeMapper;
import com.wastemanagement.backend.model.tournee.Tournee;
import com.wastemanagement.backend.model.user.Admin;
import com.wastemanagement.backend.repository.tournee.TourneeRepository;
import com.wastemanagement.backend.service.tournee.TourneeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TourneeServiceImpl implements TourneeService {

    private final TourneeRepository tourneeRepository;

    @Override
    public Tournee createTournee(TourneeRequestDTO dto) {
        Tournee tournee = TourneeMapper.toEntity(dto);
        return tourneeRepository.save(tournee);
    }

    @Override
    public Tournee updateTournee(String id, TourneeRequestDTO dto) {
        Tournee existing = tourneeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournee not found"));
        TourneeMapper.updateEntity(existing, dto);
        return tourneeRepository.save(existing);
    }

    @Override
    public Tournee getTourneeById(String id) {
        return tourneeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournee not found"));
    }

    @Override
    public List<Tournee> getAllTournees() {
        return (List<Tournee>) tourneeRepository.findAll();
    }

    @Override
    public void deleteTournee(String id) {
        tourneeRepository.deleteById(id);
    }
}
