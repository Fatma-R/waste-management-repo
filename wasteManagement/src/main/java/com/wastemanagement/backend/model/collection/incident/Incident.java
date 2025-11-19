package com.wastemanagement.backend.model.collection.incident;

import com.wastemanagement.backend.model.GeoJSONPoint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "incidents")
public abstract class Incident {
    @Id
    private String id;
    private IncidentType type;
    private Severity severity;
    private String status;
    private Date reportedAt;
    private Date resolvedAt;
    private String description;
    private GeoJSONPoint location;
}
