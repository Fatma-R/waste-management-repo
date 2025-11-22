package com.wastemanagement.backend.dto.tournee;

import com.wastemanagement.backend.model.tournee.StepStatus;
import lombok.Data;

@Data
public class RouteStepRequestDTO {
    private int order;
    private StepStatus status;
    private double predictedFillPct;
    private String notes;
    private String collectionPointId;
}
