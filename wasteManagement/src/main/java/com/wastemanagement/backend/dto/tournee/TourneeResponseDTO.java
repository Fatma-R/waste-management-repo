package com.wastemanagement.backend.dto.tournee;

import com.wastemanagement.backend.model.collection.TrashType;
import com.wastemanagement.backend.model.tournee.RouteStep;
import com.wastemanagement.backend.model.tournee.TourneeStatus;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TourneeResponseDTO {
    private String id;
    private TrashType tourneeType;
    private TourneeStatus status;
    private double plannedKm;
    private double plannedCO2;
    private String plannedVehicleId;
    private Date startedAt;
    private Date finishedAt;
    private List<RouteStepResponseDTO> steps;
    private String geometry; // encoded polyline from VROOM
}
