package com.wastemanagement.backend.model.tournee;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tourneeAssignments")
public class TourneeAssignment {

    @Id
    private String id;

    private String tourneeId;   // reference to Tournee
    private String employeeId;  // reference to Employee
    private String vehicleId;   // reference to Vehicle
    private Instant shiftStart;
    private Instant shiftEnd;
}

