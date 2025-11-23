package com.wastemanagement.backend.controller.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.service.tournee.TourneeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tournees")
@RequiredArgsConstructor
public class TourneeController {

    private final TourneeService tourneeService;

    @PostMapping
    public TourneeResponseDTO create(@RequestBody TourneeRequestDTO dto) {
        return tourneeService.createTournee(dto);
    }

    @GetMapping("/{id}")
    public TourneeResponseDTO getById(@PathVariable String id) {
        return tourneeService.getTourneeById(id);
    }

    @GetMapping
    public List<TourneeResponseDTO> getAll() {
        return tourneeService.getAllTournees();
    }

    @PutMapping("/{id}")
    public TourneeResponseDTO update(@PathVariable String id,
                                     @RequestBody TourneeRequestDTO dto) {
        return tourneeService.updateTournee(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        tourneeService.deleteTournee(id);
    }
}
