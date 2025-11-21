package com.wastemanagement.backend.mapper.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.model.tournee.Tournee;

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
        tournee.setSteps(dto.getSteps());

        return tournee;
    }

    public static void updateEntity(Tournee existing, TourneeRequestDTO dto) {
        if (existing == null || dto == null) return;

        // Update non-primitives only if non-null
        if (dto.getTourneeType() != null) existing.setTourneeType(dto.getTourneeType());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getStartedAt() != null) existing.setStartedAt(dto.getStartedAt());
        if (dto.getFinishedAt() != null) existing.setFinishedAt(dto.getFinishedAt());
        if (dto.getSteps() != null) existing.setSteps(dto.getSteps());

        // Always update primitives
        existing.setPlannedKm(dto.getPlannedKm());
        existing.setPlannedCO2(dto.getPlannedCO2());
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
        dto.setSteps(tournee.getSteps());

        return dto;
    }
}
