package com.wastemanagement.backend.service.alert;

import com.wastemanagement.backend.dto.alert.AlertRequestDTO;
import com.wastemanagement.backend.dto.alert.AlertResponseDTO;

import java.util.List;

public interface AlertService {
    AlertResponseDTO createAlert(AlertRequestDTO dto);
    List<AlertResponseDTO> getAllAlerts();
    AlertResponseDTO getAlertById(String id);
    AlertResponseDTO updateAlert(String id, AlertRequestDTO dto);
    boolean deleteAlert(String id);
    List<AlertResponseDTO> getAlertsByBinId(String binId);
    List<AlertResponseDTO> getAlertsByType(String type);
    List<AlertResponseDTO> getAlertsByCleared(boolean cleared);
}
