package com.wastemanagement.backend.service.alert;

import com.wastemanagement.backend.dto.alert.AlertRequestDTO;
import com.wastemanagement.backend.dto.alert.AlertResponseDTO;
import com.wastemanagement.backend.mapper.AlertMapper;
import com.wastemanagement.backend.model.collection.Alert;
import com.wastemanagement.backend.model.collection.AlertType;
import com.wastemanagement.backend.repository.AlertRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;

    public AlertServiceImpl(AlertRepository alertRepository, AlertMapper alertMapper) {
        this.alertRepository = alertRepository;
        this.alertMapper = alertMapper;
    }

    @Override
    public AlertResponseDTO createAlert(AlertRequestDTO dto) {
        Alert alert = alertMapper.toEntity(dto);
        return alertMapper.toResponseDTO(alertRepository.save(alert));
    }

    @Override
    public List<AlertResponseDTO> getAllAlerts() {
        return alertRepository.findAll()
                .stream()
                .map(alertMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AlertResponseDTO getAlertById(String id) {
        return alertRepository.findById(id)
                .map(alertMapper::toResponseDTO)
                .orElse(null);
    }

    @Override
    public AlertResponseDTO updateAlert(String id, AlertRequestDTO dto) {
        Alert alert = alertRepository.findById(id).orElseThrow();
        alertMapper.updateEntity(dto, alert);
        return alertMapper.toResponseDTO(alertRepository.save(alert));
    }

    @Override
    public boolean deleteAlert(String id) {
        if (alertRepository.existsById(id)) {
            alertRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<AlertResponseDTO> getAlertsByBinId(String binId) {
        return alertRepository.findByBinId(binId).stream()
                .map(alertMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertResponseDTO> getAlertsByType(String type) {
        return alertRepository.findByType(AlertType.valueOf(type.toUpperCase()))
                .stream()
                .map(alertMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertResponseDTO> getAlertsByCleared(boolean cleared) {
        return alertRepository.findByCleared(cleared).stream()
                .map(alertMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}
