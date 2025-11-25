package com.wastemanagement.backend.model.collection.incident;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleBreakdownIncident extends Incident {
    private String vehicleId;
}
