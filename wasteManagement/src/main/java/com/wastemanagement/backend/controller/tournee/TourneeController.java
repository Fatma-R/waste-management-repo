package com.wastemanagement.backend.controller.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.model.collection.TrashType;
import com.wastemanagement.backend.model.tournee.TourneeStatus;
import com.wastemanagement.backend.service.tournee.TourneeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
     * Planifie des tournées pour un ou plusieurs types de déchet donné(s),
     * en fonction d’un seuil de remplissage (fillPct),
     * crée les Tournees et retourne les TourneeResponseDTOs.
     * Exemple:
     * POST /api/v1/tournees/plan?type=PLASTIC&threshold=80 or POST api/v1/tournees/plan?types=ORGANIC&types=PLASTIC&threshold=60
     */
    @PostMapping("/plan")
    public ResponseEntity<List<TourneeResponseDTO>> planTournees(
            @RequestParam(name = "type", required = false) TrashType type,
            @RequestParam(name = "types", required = false) List<TrashType> types,
            @RequestParam(name = "threshold") double threshold
    ) {
        // 1) Resolve list of types from either `type` or `types`
        List<TrashType> resolvedTypes = new ArrayList<>();

        if (types != null) {
            resolvedTypes.addAll(types);
        }
        if (type != null) {
            resolvedTypes.add(type);
        }

        // Remove nulls & duplicates, just in case
        resolvedTypes = resolvedTypes.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (resolvedTypes.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // 2) Call the new multi-type service method
        List<TourneeResponseDTO> planned =
                tourneeService.planTourneesWithVroom(resolvedTypes, threshold);

        return ResponseEntity.ok(planned);
    }

    @GetMapping("/in-progress")
    public List<TourneeResponseDTO> getInProgressTournees() {
        return tourneeService.findByStatus(TourneeStatus.IN_PROGRESS);
    }

    @GetMapping("/7-days-co2")
    public Double getCo2Last7Days() {
        double totalCo2Grams = tourneeService.getTotalCo2ForLastDays(7);
        return totalCo2Grams / 1000.0;
    }

}
