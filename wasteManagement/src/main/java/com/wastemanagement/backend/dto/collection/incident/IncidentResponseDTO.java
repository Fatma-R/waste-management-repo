package com.wastemanagement.backend.dto.collection.incident;

import com.wastemanagement.backend.model.collection.incident.IncidentSeverity;
import com.wastemanagement.backend.model.collection.incident.IncidentStatus;
import com.wastemanagement.backend.model.collection.incident.IncidentType;
import lombok.Data;

import java.time.Instant;

@Data
public class IncidentResponseDTO {
    private String id;
    private IncidentType type;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private Instant reportedAt;
    private Instant resolvedAt;
    private String description;
    private GeoJSONPointDTO location;

}
