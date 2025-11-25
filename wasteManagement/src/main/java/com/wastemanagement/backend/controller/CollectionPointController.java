package com.wastemanagement.backend.controller;


import com.wastemanagement.backend.dto.collectionPoint.CollectionPointRequestDTO;
import com.wastemanagement.backend.dto.collectionPoint.CollectionPointResponseDTO;
import com.wastemanagement.backend.service.collectionPoint.CollectionPointService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/collectionPoints")
public class CollectionPointController {

    private final CollectionPointService service;

    public CollectionPointController(CollectionPointService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CollectionPointResponseDTO> create(@RequestBody CollectionPointRequestDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionPointResponseDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<CollectionPointResponseDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollectionPointResponseDTO> update(@PathVariable String id, @RequestBody CollectionPointRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
