package com.wastemanagement.backend.controller.collection;


import com.wastemanagement.backend.dto.collection.BinReadingRequestDTO;
import com.wastemanagement.backend.dto.collection.BinReadingResponseDTO;
import com.wastemanagement.backend.service.collection.BinReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bin-readings")
@RequiredArgsConstructor
public class BinReadingController {

    private final BinReadingService service;

    @PostMapping
    public BinReadingResponseDTO create(@RequestBody BinReadingRequestDTO dto) {
        return service.create(dto);
    }

    @GetMapping
    public List<BinReadingResponseDTO> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public BinReadingResponseDTO getById(@PathVariable String id) {
        return service.getById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
