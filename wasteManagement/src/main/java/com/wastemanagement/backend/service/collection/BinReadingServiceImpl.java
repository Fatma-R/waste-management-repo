package com.wastemanagement.backend.service.collection;

import com.wastemanagement.backend.dto.alert.AlertRequestDTO;
import com.wastemanagement.backend.dto.collection.BinReadingRequestDTO;
import com.wastemanagement.backend.dto.collection.BinReadingResponseDTO;
import com.wastemanagement.backend.mapper.collection.BinReadingMapper;
import com.wastemanagement.backend.model.collection.AlertType;
import com.wastemanagement.backend.model.collection.BinReading;
import com.wastemanagement.backend.repository.collection.BinReadingRepository;
import com.wastemanagement.backend.service.alert.AlertService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BinReadingServiceImpl implements BinReadingService {

    private final BinReadingRepository repository;
    private final AlertService alertService;

    public BinReadingServiceImpl(BinReadingRepository repository, AlertService alertService) {
        this.repository = repository;
        this.alertService = alertService;
    }

    @Override
    public BinReadingResponseDTO create(BinReadingRequestDTO dto) {
        BinReading entity = BinReadingMapper.toEntity(dto);
        BinReading savedReading = repository.save(entity);
        
        // Evaluate alerts after saving the reading
        evaluateAlerts(savedReading);
        
        return BinReadingMapper.toResponseDTO(savedReading);
    }

    @Override
    public List<BinReadingResponseDTO> getAll() {
        return repository.findAll().stream()
                .map(BinReadingMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BinReadingResponseDTO getById(String id) {
        return repository.findById(id)
                .map(BinReadingMapper::toResponseDTO)
                .orElseGet(() -> {
                    System.out.println("BinReading not found for id: " + id);
                    return null;
                });}

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }

    /**
     * Evaluates sensor readings and triggers appropriate alerts
     * Rules:
     * - If fillPct >= 95 → LEVEL_CRITICAL
     * - Else if fillPct >= 80 → LEVEL_HIGH
     * - If batteryPct <= 20 → BATTERY_LOW
     * - If temperatureC > 60 → SENSOR_ANOMALY
     */
    private void evaluateAlerts(BinReading reading) {
        try {
            // Evaluate fill level alerts
            if (reading.getFillPct() >= 95) {
                createAlert(reading, AlertType.LEVEL_CRITICAL, reading.getFillPct());
            } else if (reading.getFillPct() >= 80) {
                createAlert(reading, AlertType.LEVEL_HIGH, reading.getFillPct());
            }

            // Evaluate battery level alerts
            if (reading.getBatteryPct() <= 20) {
                createAlert(reading, AlertType.BATTERY_LOW, reading.getBatteryPct());
            }

            // Evaluate temperature anomalies
            if (reading.getTemperatureC() > 60) {
                createAlert(reading, AlertType.SENSOR_ANOMALY, reading.getTemperatureC());
            }
        } catch (Exception e) {
            // Log error but don't fail the reading creation
            System.err.println("Error evaluating alerts for reading: " + reading.getId() + " - " + e.getMessage());
        }
    }

    /**
     * Helper method to create an alert
     */
    private void createAlert(BinReading reading, AlertType alertType, double value) {
        AlertRequestDTO alertDTO = new AlertRequestDTO();
        alertDTO.setBinId(reading.getBinId());
        alertDTO.setTs(reading.getTs());
        alertDTO.setType(alertType);
        alertDTO.setValue(value);
        alertDTO.setCleared(false);
        
        alertService.createAlert(alertDTO);
    }

    @Override
    public BinReadingResponseDTO findTopByBinIdOrderByTsDesc(String binId) {
        BinReading entity = repository.findTopByBinIdOrderByTsDesc(binId);
        if (entity == null) {
            throw new RuntimeException("BinReading not found");
        }
        return BinReadingMapper.toResponseDTO(entity);
    }
}
