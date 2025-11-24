package com.wastemanagement.backend.controller;


import com.wastemanagement.backend.dto.vehicle.VehicleRequestDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleResponseDTO;
import com.wastemanagement.backend.model.vehicle.FuelType;
import com.wastemanagement.backend.service.vehicle.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService service;
    @PostMapping
    public ResponseEntity<VehicleResponseDTO> create(@RequestBody VehicleRequestDTO dto) {
        return ResponseEntity.ok(service.createVehicle(dto));
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponseDTO>> getAll(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getAllVehicles(page, size));
    }
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> getById(@PathVariable String id) {
        VehicleResponseDTO dto = service.getVehicleById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> update(@PathVariable String id, @RequestBody VehicleRequestDTO dto) {
        VehicleResponseDTO updated = service.updateVehicle(id, dto);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return service.deleteVehicle(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<VehicleResponseDTO>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(service.getVehiclesByStatus(status));
    }

    @GetMapping("/fuel/{fuelType}")
    public ResponseEntity<List<VehicleResponseDTO>> getByFuelType(@PathVariable FuelType fuelType) {
        return ResponseEntity.ok(service.getVehiclesByFuelType(fuelType));
    }
}
