package com.wastemanagement.backend.dto.tournee;

import lombok.Data;
import java.util.Date;

@Data
public class TourneeAssignmentRequestDTO {
    private String tourneeId;
    private String employeeId;
    private String vehicleId;
    private Date shiftStart;
    private Date shiftEnd;
}
