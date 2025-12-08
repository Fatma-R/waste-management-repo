package com.wastemanagement.backend.controller.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeAssignmentRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeAssignmentResponseDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.service.tournee.TourneeAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tournee-assignments")
public class TourneeAssignmentController {

    private final TourneeAssignmentService service;

    public TourneeAssignmentController(TourneeAssignmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<TourneeAssignmentResponseDTO> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TourneeAssignmentResponseDTO> getById(@PathVariable String id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public TourneeAssignmentResponseDTO create(@RequestBody TourneeAssignmentRequestDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TourneeAssignmentResponseDTO> update(
            @PathVariable String id,
            @RequestBody TourneeAssignmentRequestDTO dto
    ) {
        return service.update(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/{tourneeId}/assignments/auto")
    public ResponseEntity<List<TourneeAssignmentResponseDTO>> autoAssign(
            @PathVariable String tourneeId
    ) {
        List<TourneeAssignmentResponseDTO> created =
                service.autoAssignForTournee(tourneeId);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/employee/{employeeId}/tournees/in-progress")
    public List<TourneeResponseDTO> getInProgressTournees(@PathVariable String employeeId) {
        return service.getInProgressTourneesForEmployee(employeeId);
    }

    @GetMapping("/tournee/{tourneeId}")
    public List<TourneeAssignmentResponseDTO> getAssignmentsForTournee(@PathVariable String tourneeId) {
        return service.getAssignmentsForTournee(tourneeId);
    }

}
