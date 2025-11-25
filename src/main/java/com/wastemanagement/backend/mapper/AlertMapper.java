package com.wastemanagement.backend.mapper;

import com.wastemanagement.backend.dto.alert.AlertRequestDTO;
import com.wastemanagement.backend.dto.alert.AlertResponseDTO;
import com.wastemanagement.backend.model.collection.Alert;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    public AlertResponseDTO toResponseDTO(Alert alert) {
        if (alert == null) return null;
        return new AlertResponseDTO(
                alert.getId(),
                alert.getBinId(),
                alert.getTs(),
                alert.getType(),
                alert.getValue(),
                alert.isCleared()
        );
    }

    public Alert toEntity(AlertRequestDTO dto) {
        if (dto == null) return null;
        return new Alert(
                null,
                dto.getBinId(),
                dto.getTs(),
                dto.getType(),
                dto.getValue(),
                dto.isCleared()
        );
    }

    public void updateEntity(AlertRequestDTO dto, Alert entity) {
        if (dto.getBinId() != null) entity.setBinId(dto.getBinId());
        if (dto.getTs() != null) entity.setTs(dto.getTs());
        if (dto.getType() != null) entity.setType(dto.getType());
        entity.setValue(dto.getValue());
        entity.setCleared(dto.isCleared());
    }
}
