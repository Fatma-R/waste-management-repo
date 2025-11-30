package com.wastemanagement.backend.dto.tournee;

import lombok.Data;

import java.time.Instant;

@Data
public class TourneeAssignmentResponseDTO {
    private String id;
    private String tourneeId;
    private String employeeId;
    private String vehicleId;
    private Instant shiftStart;
    private Instant shiftEnd;
}
