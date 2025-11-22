package com.wastemanagement.backend.mapper.tournee;

import com.wastemanagement.backend.dto.tournee.RouteStepRequestDTO;
import com.wastemanagement.backend.dto.tournee.RouteStepResponseDTO;
import com.wastemanagement.backend.model.tournee.RouteStep;

public class RouteStepMapper {

    public static RouteStep toEntity(RouteStepRequestDTO dto) {
        if (dto == null) return null;

        RouteStep step = new RouteStep();
        step.setOrder(dto.getOrder());
        step.setStatus(dto.getStatus());
        step.setPredictedFillPct(dto.getPredictedFillPct());
        step.setNotes(dto.getNotes());
        step.setCollectionPointId(dto.getCollectionPointId());
        return step;
    }

    public static void updateEntity(RouteStep step, RouteStepRequestDTO dto) {
        // Always update primitives
        step.setOrder(dto.getOrder());
        step.setPredictedFillPct(dto.getPredictedFillPct());

        // Update non-primitives only if non-null
        if (dto.getStatus() != null) step.setStatus(dto.getStatus());
        if (dto.getNotes() != null) step.setNotes(dto.getNotes());
        if (dto.getCollectionPointId() != null) step.setCollectionPointId(dto.getCollectionPointId());
    }

    public static RouteStepResponseDTO toResponse(RouteStep step) {
        if (step == null) return null;

        RouteStepResponseDTO dto = new RouteStepResponseDTO();
        dto.setId(step.getId());
        dto.setOrder(step.getOrder());
        dto.setStatus(step.getStatus());
        dto.setPredictedFillPct(step.getPredictedFillPct());
        dto.setNotes(step.getNotes());
        dto.setCollectionPointId(step.getCollectionPointId());
        return dto;
    }
}
