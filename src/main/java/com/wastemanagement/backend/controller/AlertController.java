package com.wastemanagement.backend.controller;

import com.wastemanagement.backend.dto.alert.AlertRequestDTO;
import com.wastemanagement.backend.dto.alert.AlertResponseDTO;
import com.wastemanagement.backend.service.alert.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    public ResponseEntity<AlertResponseDTO> createAlert(@RequestBody AlertRequestDTO dto) {
        return ResponseEntity.ok(alertService.createAlert(dto));
    }

    @GetMapping
    public ResponseEntity<List<AlertResponseDTO>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponseDTO> getAlertById(@PathVariable String id) {
        return ResponseEntity.ok(alertService.getAlertById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertResponseDTO> updateAlert(@PathVariable String id, @RequestBody AlertRequestDTO dto) {
        return ResponseEntity.ok(alertService.updateAlert(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable String id) {
        if (alertService.deleteAlert(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/bin/{binId}")
    public ResponseEntity<List<AlertResponseDTO>> getAlertsByBin(@PathVariable String binId) {
        return ResponseEntity.ok(alertService.getAlertsByBinId(binId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<AlertResponseDTO>> getAlertsByType(@PathVariable String type) {
        return ResponseEntity.ok(alertService.getAlertsByType(type));
    }

    @GetMapping("/cleared/{cleared}")
    public ResponseEntity<List<AlertResponseDTO>> getAlertsByCleared(@PathVariable boolean cleared) {
        return ResponseEntity.ok(alertService.getAlertsByCleared(cleared));
    }
}
