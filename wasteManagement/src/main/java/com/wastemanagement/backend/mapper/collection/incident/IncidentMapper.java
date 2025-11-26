package com.wastemanagement.backend.mapper.collection.incident;

import com.wastemanagement.backend.dto.GeoJSONPointDTO;
import com.wastemanagement.backend.dto.collection.incident.IncidentRequestDTO;
import com.wastemanagement.backend.dto.collection.incident.IncidentResponseDTO;
import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.collection.incident.Incident;

public class IncidentMapper {

    public static Incident toEntity(IncidentRequestDTO dto) {
        if (dto == null) return null;

        Incident incident = new Incident();
        incident.setType(dto.getType());
        incident.setSeverity(dto.getSeverity());
        incident.setStatus(dto.getStatus());
        incident.setReportedAt(dto.getReportedAt());
        incident.setResolvedAt(dto.getResolvedAt());
        incident.setDescription(dto.getDescription());
        incident.setLocation(toGeoJSONPoint(dto.getLocation()));

        return incident;
    }

    public static void updateEntity(Incident existing, IncidentRequestDTO dto) {
        if (existing == null || dto == null) return;

        if (dto.getType() != null) {
            existing.setType(dto.getType());
        }
        if (dto.getSeverity() != null) {
            existing.setSeverity(dto.getSeverity());
        }
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        if (dto.getReportedAt() != null) {
            existing.setReportedAt(dto.getReportedAt());
        }
        if (dto.getResolvedAt() != null) {
            existing.setResolvedAt(dto.getResolvedAt());
        }
        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }
        if (dto.getLocation() != null && dto.getLocation().getCoordinates() != null) {
            existing.setLocation(toGeoJSONPoint(dto.getLocation()));
        }
    }

    public static IncidentResponseDTO toResponse(Incident incident) {
        if (incident == null) return null;

        IncidentResponseDTO dto = new IncidentResponseDTO();
        dto.setId(incident.getId());
        dto.setType(incident.getType());
        dto.setSeverity(incident.getSeverity());
        dto.setStatus(incident.getStatus());
        dto.setReportedAt(incident.getReportedAt());
        dto.setResolvedAt(incident.getResolvedAt());
        dto.setDescription(incident.getDescription());
        dto.setLocation(toGeoJSONPointDTO(incident.getLocation()));

        return dto;
    }

    private static GeoJSONPoint toGeoJSONPoint(GeoJSONPointDTO dto) {
        if (dto == null) return null;
        double[] coords = dto.getCoordinates();
        if (coords == null || coords.length != 2) return null;

        // note: coords[0] = longitude, coords[1] = latitude
        GeoJSONPoint point = new GeoJSONPoint();
        point.setType("Point");
        point.setCoordinates(new double[]{coords[0], coords[1]});
        return point;
    }

    private static GeoJSONPointDTO toGeoJSONPointDTO(GeoJSONPoint point) {
        if (point == null || point.getCoordinates() == null) return null;
        GeoJSONPointDTO dto = new GeoJSONPointDTO();
        dto.setCoordinates(point.getCoordinates());
        return dto;
    }
}
