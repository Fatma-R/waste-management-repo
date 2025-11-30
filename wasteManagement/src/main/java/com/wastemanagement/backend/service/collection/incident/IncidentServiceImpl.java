package com.wastemanagement.backend.service.collection.incident;


import com.wastemanagement.backend.dto.collection.incident.IncidentRequestDTO;
import com.wastemanagement.backend.dto.collection.incident.IncidentResponseDTO;
import com.wastemanagement.backend.mapper.collection.incident.IncidentMapper;
import com.wastemanagement.backend.model.collection.incident.Incident;
import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.model.collection.CollectionPoint;
import com.wastemanagement.backend.repository.collection.incident.IncidentRepository;
import com.wastemanagement.backend.repository.CollectionPointRepository;
import com.wastemanagement.backend.repository.collection.BinRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final CollectionPointRepository collectionPointRepository;
    private final BinRepository binRepository;

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
    public void deleteIncident(String id) {
        if (!incidentRepository.existsById(id)) {
            throw new RuntimeException("Incident not found: " + id);
        }
        incidentRepository.deleteById(id);
    }

    /**
     * Handles incident impact propagation by deactivating affected bins.
     * 
     * Process:
     * 1. Find all collection points within the incident's impact radius
     * 2. For each affected collection point, find all its bins
     * 3. Set each bin's active flag to false and save
     * 
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

            // For each impacted collection point, deactivate its bins
            for (CollectionPoint collectionPoint : impactedPoints) {
                deactivateBinsForCollectionPoint(collectionPoint.getId());
            }

        } catch (Exception e) {
            // Log error but don't fail the incident creation
            System.err.println("Error handling incident impact for incident: " + incident.getId() 
                    + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Deactivates all bins associated with a collection point.
     * 
     */
    private void deactivateBinsForCollectionPoint(String collectionPointId) {
        // Fetch all bins for this collection point
        List<Bin> bins = binRepository.findByCollectionPointId(collectionPointId);

        // Deactivate each bin and save
        for (Bin bin : bins) {
            bin.setActive(false);
            binRepository.save(bin);
        }
    }
}
