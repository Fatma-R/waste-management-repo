package com.wastemanagement.backend.controller.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.mapper.tournee.TourneeMapper;
import com.wastemanagement.backend.model.tournee.Tournee;
import com.wastemanagement.backend.service.tournee.TourneeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tournees")
@RequiredArgsConstructor
public class TourneeController {

    private final TourneeService tourneeService;

    @PostMapping
    public TourneeResponseDTO create(@RequestBody TourneeRequestDTO dto) {
        Tournee t = tourneeService.createTournee(dto);
        return TourneeMapper.toResponse(t);
    }

    @GetMapping("/{id}")
    public TourneeResponseDTO getById(@PathVariable String id) {
        Tournee t = tourneeService.getTourneeById(id);
        return TourneeMapper.toResponse(t);
    }

    @GetMapping
    public List<TourneeResponseDTO> getAll() {
        return tourneeService.getAllTournees()
                .stream()
                .map(TourneeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public TourneeResponseDTO update(@PathVariable String id,
                                     @RequestBody TourneeRequestDTO dto) {
        Tournee updated = tourneeService.updateTournee(id, dto);
        return TourneeMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        tourneeService.deleteTournee(id);
    }
}
