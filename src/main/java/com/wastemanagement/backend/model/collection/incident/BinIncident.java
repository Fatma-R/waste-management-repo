package com.wastemanagement.backend.model.collection.incident;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BinIncident extends Incident {
    private String binId;
    private String collectionPointId;
}
