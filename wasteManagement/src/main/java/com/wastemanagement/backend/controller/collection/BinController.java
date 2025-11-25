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
@RequestMapping("/api/v1/bins")
public class BinController {

    private final BinService binService;

    public BinController(BinService binService) {
        this.binService = binService;
    }

    @GetMapping
    public List<BinResponseDTO> getAllBins() {
        return binService.getAllBins();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BinResponseDTO> getBinById(@PathVariable String id) {
        return binService.getBinById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public BinResponseDTO createBin(@RequestBody BinRequestDTO dto) {
        return binService.createBin(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BinResponseDTO> updateBin(@PathVariable String id,
                                                    @RequestBody BinRequestDTO dto) {
        return binService.updateBin(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBin(@PathVariable String id) {
        return binService.deleteBin(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
