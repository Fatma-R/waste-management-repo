package com.wastemanagement.backend.service.collection.incident;


import com.wastemanagement.backend.dto.collection.incident.IncidentRequestDTO;
import com.wastemanagement.backend.dto.collection.incident.IncidentResponseDTO;
import com.wastemanagement.backend.mapper.collection.incident.IncidentMapper;
import com.wastemanagement.backend.model.collection.incident.Incident;
import com.wastemanagement.backend.repository.collection.incident.IncidentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;

    @Override
    public IncidentResponseDTO createIncident(IncidentRequestDTO dto) {
        Incident incident = IncidentMapper.toEntity(dto);
        incident = incidentRepository.save(incident);
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
}
