package com.wastemanagement.backend.mapper.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.dto.tournee.RouteStepRequestDTO;
import com.wastemanagement.backend.dto.tournee.RouteStepResponseDTO;
import com.wastemanagement.backend.model.tournee.Tournee;
import com.wastemanagement.backend.model.tournee.RouteStep;

import java.util.List;
import java.util.stream.Collectors;

public class TourneeMapper {

    public static Tournee toEntity(TourneeRequestDTO dto) {
        if (dto == null) return null;

        Tournee tournee = new Tournee();
        tournee.setTourneeType(dto.getTourneeType());
        tournee.setStatus(dto.getStatus());
        tournee.setPlannedKm(dto.getPlannedKm());
        tournee.setPlannedCO2(dto.getPlannedCO2());
        tournee.setStartedAt(dto.getStartedAt());
        tournee.setFinishedAt(dto.getFinishedAt());
        tournee.setPlannedVehicleId(dto.getPlannedVehicleId());

        // Map steps
        if (dto.getSteps() != null) {
            List<RouteStep> steps = dto.getSteps()
                    .stream()
                    .map(RouteStepMapper::toEntity)
                    .collect(Collectors.toList());
            tournee.setSteps(steps);
        }

        return tournee;
    }

    public static void updateEntity(Tournee existing, TourneeRequestDTO dto) {
        if (existing == null || dto == null) return;

        // Update non-primitives only if non-null
        if (dto.getTourneeType() != null) existing.setTourneeType(dto.getTourneeType());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getStartedAt() != null) existing.setStartedAt(dto.getStartedAt());
        if (dto.getFinishedAt() != null) existing.setFinishedAt(dto.getFinishedAt());

        if (dto.getSteps() != null) {
            List<RouteStep> steps = dto.getSteps()
                    .stream()
                    .map(RouteStepMapper::toEntity)
                    .collect(Collectors.toList());
            existing.setSteps(steps);
        }

        // Always update primitives
        existing.setPlannedKm(dto.getPlannedKm());
        existing.setPlannedCO2(dto.getPlannedCO2());
        existing.setPlannedVehicleId(dto.getPlannedVehicleId());
    }

    public static TourneeResponseDTO toResponse(Tournee tournee) {
        if (tournee == null) return null;

        TourneeResponseDTO dto = new TourneeResponseDTO();
        dto.setId(tournee.getId());
        dto.setTourneeType(tournee.getTourneeType());
        dto.setStatus(tournee.getStatus());
        dto.setPlannedKm(tournee.getPlannedKm());
        dto.setPlannedCO2(tournee.getPlannedCO2());
        dto.setStartedAt(tournee.getStartedAt());
        dto.setFinishedAt(tournee.getFinishedAt());
        dto.setGeometry(tournee.getGeometry());
        dto.setPlannedVehicleId(tournee.getPlannedVehicleId());


        // Map steps to response DTOs
        if (tournee.getSteps() != null) {
            List<RouteStepResponseDTO> steps = tournee.getSteps()
                    .stream()
                    .map(RouteStepMapper::toResponse)
                    .collect(Collectors.toList());
            dto.setSteps(steps);
        }

        return dto;
    }
}
