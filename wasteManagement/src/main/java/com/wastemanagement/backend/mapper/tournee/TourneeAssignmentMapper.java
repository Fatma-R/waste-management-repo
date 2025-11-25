package com.wastemanagement.backend.mapper.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeAssignmentRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeAssignmentResponseDTO;
import com.wastemanagement.backend.model.tournee.TourneeAssignment;

public class TourneeAssignmentMapper {

    public static TourneeAssignment toEntity(TourneeAssignmentRequestDTO dto) {
        TourneeAssignment t = new TourneeAssignment();
        t.setTourneeId(dto.getTourneeId());
        t.setEmployeeId(dto.getEmployeeId());
        t.setVehicleId(dto.getVehicleId());
        t.setShiftStart(dto.getShiftStart());
        t.setShiftEnd(dto.getShiftEnd());
        return t;
    }

    public static void merge(TourneeAssignment existing, TourneeAssignmentRequestDTO dto) {
        existing.setTourneeId(dto.getTourneeId());
        existing.setEmployeeId(dto.getEmployeeId());
        existing.setVehicleId(dto.getVehicleId());
        existing.setShiftStart(dto.getShiftStart());
        existing.setShiftEnd(dto.getShiftEnd());
    }

    public static TourneeAssignmentResponseDTO toResponseDTO(TourneeAssignment entity) {
        TourneeAssignmentResponseDTO dto = new TourneeAssignmentResponseDTO();
        dto.setId(entity.getId());
        dto.setTourneeId(entity.getTourneeId());
        dto.setEmployeeId(entity.getEmployeeId());
        dto.setVehicleId(entity.getVehicleId());
        dto.setShiftStart(entity.getShiftStart());
        dto.setShiftEnd(entity.getShiftEnd());
        return dto;
    }
}
