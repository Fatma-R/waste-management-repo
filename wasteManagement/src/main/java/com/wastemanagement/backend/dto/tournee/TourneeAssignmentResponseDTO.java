package com.wastemanagement.backend.dto.tournee;

import lombok.Data;
import java.util.Date;

@Data
public class TourneeAssignmentResponseDTO {
    private String id;
    private String tourneeId;
    private String employeeId;
    private String vehicleId;
    private Date shiftStart;
    private Date shiftEnd;
}
