package com.wastemanagement.backend.model.tournee;

import com.wastemanagement.backend.model.collection.TrashType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tournees")
public class Tournee {
    @Id
    private String id;
    private TrashType tourneeType;
    private TourneeStatus status;
    private double plannedKm;
    private double plannedCO2;
    private String plannedVehicleId;
    private Date startedAt;
    private Date finishedAt;
    private List<RouteStep> steps;
    private String geometry;
}
