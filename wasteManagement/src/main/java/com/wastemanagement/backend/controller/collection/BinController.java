package com.wastemanagement.backend.controller.collection;

import com.wastemanagement.backend.dto.collection.BinRequestDTO;
import com.wastemanagement.backend.dto.collection.BinResponseDTO;
import com.wastemanagement.backend.mapper.collection.BinMapper;
import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.service.collection.BinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bins")
public class BinController {

    private final BinService binService;

    public BinController(BinService binService) {
        this.binService = binService;
    }

    @GetMapping
    public List<BinResponseDTO> getAllBins() {
        return binService.getAllBins()
                .stream()
                .map(BinMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BinResponseDTO> getBinById(@PathVariable String id) {
        return binService.getBinById(id)
                .map(BinMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public BinResponseDTO createBin(@RequestBody BinRequestDTO dto) {
        Bin saved = binService.createBin(BinMapper.toEntity(dto));
        return BinMapper.toResponseDTO(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BinResponseDTO> updateBin(@PathVariable String id, @RequestBody BinRequestDTO dto) {
        return binService.getBinById(id)
                .map(existing -> {
                    BinMapper.merge(existing, dto);
                    Bin updated = binService.createBin(existing);
                    return ResponseEntity.ok(BinMapper.toResponseDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBin(@PathVariable String id) {
        return binService.deleteBin(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
