package com.wastemanagement.backend.model.tournee;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "routeSteps")
public class RouteStep {
    @Id
    private String id;
    private int order;
    private StepStatus status;
    private double predictedFillPct;
    private String notes;
    private String collectionPointId;
}
