package com.wastemanagement.backend.controller.tournee;

import com.wastemanagement.backend.dto.tournee.DepotRequestDTO;
import com.wastemanagement.backend.dto.tournee.DepotResponseDTO;
import com.wastemanagement.backend.service.tournee.DepotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/depots")
public class DepotController {

    private final DepotService depotService;

    public DepotController(DepotService depotService) {
        this.depotService = depotService;
    }

    @PostMapping
    public ResponseEntity<DepotResponseDTO> createDepot(@RequestBody DepotRequestDTO requestDTO) {
        DepotResponseDTO created = depotService.createDepot(requestDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepotResponseDTO> getDepotById(@PathVariable String id) {
        DepotResponseDTO depot = depotService.getDepotById(id);
        if (depot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(depot);
    }

    @GetMapping
    public ResponseEntity<List<DepotResponseDTO>> getAllDepots() {
        List<DepotResponseDTO> depots = depotService.getAllDepots();
        return ResponseEntity.ok(depots);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepotResponseDTO> updateDepot(
            @PathVariable String id,
            @RequestBody DepotRequestDTO requestDTO
    ) {
        DepotResponseDTO updated = depotService.updateDepot(id, requestDTO);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepot(@PathVariable String id) {
        depotService.deleteDepot(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère le dépôt principal (id = MAIN_DEPOT).
     * GET /api/v1/depots/main
     */
    @GetMapping("/main")
    public ResponseEntity<DepotResponseDTO> getMainDepot() {
        return depotService.getMainDepot()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crée ou met à jour le dépôt principal (id = MAIN_DEPOT).
     * PUT /api/v1/depots/main
     */
    @PutMapping("/main")
    public ResponseEntity<DepotResponseDTO> saveOrUpdateMainDepot(
            @RequestBody DepotRequestDTO requestDTO
    ) {
        DepotResponseDTO updated = depotService.saveOrUpdateMainDepot(requestDTO);
        return ResponseEntity.ok(updated);
    }

}
