package com.wastemanagement.backend.service.collection.incident;

import com.wastemanagement.backend.dto.collection.incident.IncidentRequestDTO;
import com.wastemanagement.backend.dto.collection.incident.IncidentResponseDTO;
import java.util.List;

public interface IncidentService {

    IncidentResponseDTO createIncident(IncidentRequestDTO dto);

    IncidentResponseDTO getIncident(String id);

    List<IncidentResponseDTO> getAllIncidents();

    IncidentResponseDTO updateIncident(String id, IncidentRequestDTO dto);

    IncidentResponseDTO resolveIncident(String id);

    void deleteIncident(String id);
}
