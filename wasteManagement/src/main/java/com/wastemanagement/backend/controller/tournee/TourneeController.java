package com.wastemanagement.backend.controller.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.model.collection.TrashType;
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

    // planifier une tournée à partir de VROOM

    /**
     * Planifie une tournée pour un type de déchet donné,
     * en fonction d’un seuil de remplissage (fillPct),
     * crée la Tournee en base et retourne le TourneeResponseDTO.
     *
     * Exemple:
     * POST /api/v1/tournees/plan?type=PLASTIC&threshold=80
     */
    @PostMapping("/plan")
    public TourneeResponseDTO planWithVroom(
            @RequestParam TrashType type,
            @RequestParam(defaultValue = "80") double threshold
    ) {
        return tourneeService.planTourneeWithVroom(type, threshold);
    }
}
