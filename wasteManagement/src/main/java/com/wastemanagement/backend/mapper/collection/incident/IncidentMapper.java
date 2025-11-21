package com.wastemanagement.backend.mapper.collection.incident;

import com.wastemanagement.backend.dto.collection.incident.*;
import com.wastemanagement.backend.model.collection.incident.*;
import org.modelmapper.ModelMapper;

public class IncidentMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    // ----------------------------
    // Convert RequestDTO -> Entity
    // ----------------------------

    public static Incident toEntity(IncidentRequestDTO dto) {
        if (dto == null) return null;

        switch (dto.getType()) {

            case BIN_DAMAGED:
                BinIncident binIncident = modelMapper.map(dto, BinIncident.class);
                return binIncident;

            case BLOCKED_STREET:
                BlockedStreetIncident streetIncident = modelMapper.map(dto, BlockedStreetIncident.class);
                return streetIncident;

            case VEHICLE_BREAKDOWN:
                VehicleBreakdownIncident vehicleIncident = modelMapper.map(dto, VehicleBreakdownIncident.class);
                return vehicleIncident;

            default:
                throw new IllegalArgumentException("Unknown incident type: " + dto.getType());
        }
    }

    // ----------------------------
    // Update an existing entity
    // ----------------------------

    public static void updateEntity(Incident existing, IncidentRequestDTO dto) {
        if (existing == null || dto == null) return;

        modelMapper.map(dto, existing);
    }

    // ----------------------------
    // Entity -> ResponseDTO
    // ----------------------------

    public static IncidentResponseDTO toResponse(Incident incident) {
        if (incident == null) return null;

        IncidentResponseDTO dto = modelMapper.map(incident, IncidentResponseDTO.class);

        // Polymorphic information
        dto.setType(incident.getType());

        return dto;
    }
}
