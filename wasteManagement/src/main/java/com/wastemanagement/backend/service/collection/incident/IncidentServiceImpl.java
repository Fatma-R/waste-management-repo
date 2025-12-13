package com.wastemanagement.backend.service.collection.incident;


import com.wastemanagement.backend.dto.collection.incident.IncidentRequestDTO;
import com.wastemanagement.backend.dto.collection.incident.IncidentResponseDTO;
import com.wastemanagement.backend.mapper.collection.incident.IncidentMapper;
import com.wastemanagement.backend.model.collection.incident.Incident;
import com.wastemanagement.backend.model.collection.incident.IncidentStatus;
import com.wastemanagement.backend.model.collection.CollectionPoint;
import com.wastemanagement.backend.repository.collection.incident.IncidentRepository;
import com.wastemanagement.backend.repository.CollectionPointRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final CollectionPointRepository collectionPointRepository;

    // Configurable impact radius in meters (default: 500m)
    private static final long INCIDENT_IMPACT_RADIUS_METERS = 500;

    @Override
    public IncidentResponseDTO createIncident(IncidentRequestDTO dto) {
        Incident incident = IncidentMapper.toEntity(dto);
        incident = incidentRepository.save(incident);
        
        // Handle incident impact propagation
        handleIncidentImpact(incident);
        
        return IncidentMapper.toResponse(incident);
    }

    @Override
    public IncidentResponseDTO getIncident(String id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + id));
        return IncidentMapper.toResponse(incident);
    }

    @Override
    public List<IncidentResponseDTO> getAllIncidents() {
        return incidentRepository.findAll().stream()
                .map(IncidentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public IncidentResponseDTO updateIncident(String id, IncidentRequestDTO dto) {
        Incident existing = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + id));

        IncidentMapper.updateEntity(existing, dto);
        incidentRepository.save(existing);

        return IncidentMapper.toResponse(existing);
    }

    @Override
    public IncidentResponseDTO resolveIncident(String id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + id));

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(Instant.now());
        incidentRepository.save(incident);

        reactivateImpactedCollectionPoints(incident);

        return IncidentMapper.toResponse(incident);
    }

    @Override
    public void deleteIncident(String id) {
        if (!incidentRepository.existsById(id)) {
            throw new RuntimeException("Incident not found: " + id);
        }
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + id));
        reactivateImpactedCollectionPoints(incident);
        incidentRepository.deleteById(id);
    }

    /**
     * Handles incident impact propagation by deactivating affected collection points.
     *
     * Process:
     * 1. Find all collection points within the incident's impact radius
     * 2. Set each impacted collection point's active flag to false and persist
     */
    private void handleIncidentImpact(Incident incident) {
        try {
            // Validate incident has location
            if (incident.getLocation() == null) {
                return;
            }

            // Find all collection points within impact radius
            List<CollectionPoint> impactedPoints = collectionPointRepository
                    .findNearby(incident.getLocation(), INCIDENT_IMPACT_RADIUS_METERS);

            // For each impacted collection point, deactivate it
            for (CollectionPoint collectionPoint : impactedPoints) {
                collectionPoint.setActive(false);
                collectionPointRepository.save(collectionPoint);
            }

        } catch (Exception e) {
            // Log error but don't fail the incident creation
            System.err.println("Error handling incident impact for incident: " + incident.getId() 
                    + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reactivates collection points within the incident's impact radius (used when incident is resolved).
     */
    private void reactivateImpactedCollectionPoints(Incident incident) {
        if (incident.getLocation() == null) {
            return;
        }

        List<CollectionPoint> impactedPoints = collectionPointRepository
                .findNearby(incident.getLocation(), INCIDENT_IMPACT_RADIUS_METERS);

        for (CollectionPoint collectionPoint : impactedPoints) {
            collectionPoint.setActive(true);
            collectionPointRepository.save(collectionPoint);
        }
    }
}
