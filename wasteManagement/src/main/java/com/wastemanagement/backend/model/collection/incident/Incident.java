package com.wastemanagement.backend.model.collection.incident;

import com.wastemanagement.backend.model.GeoJSONPoint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "incidents")
public class Incident {
    @Id
    private String id;
    private IncidentType type;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private Instant reportedAt;
    private Instant resolvedAt;
    private String description;
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJSONPoint location;



}