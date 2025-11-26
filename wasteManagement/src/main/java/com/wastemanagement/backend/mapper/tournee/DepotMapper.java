package com.wastemanagement.backend.mapper.tournee;

import com.wastemanagement.backend.dto.GeoJSONPointDTO;
import com.wastemanagement.backend.dto.tournee.DepotRequestDTO;
import com.wastemanagement.backend.dto.tournee.DepotResponseDTO;
import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.tournee.Depot;
import org.springframework.stereotype.Component;

@Component
public class DepotMapper {

    public Depot toEntity(DepotRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Depot depot = new Depot();
        depot.setId(dto.getId());
        depot.setName(dto.getName());
        depot.setAddress(dto.getAddress());
        depot.setLocation(toGeoJSONPoint(dto.getLocation()));

        return depot;
    }

    public DepotResponseDTO toResponseDTO(Depot entity) {
        if (entity == null) {
            return null;
        }

        DepotResponseDTO dto = new DepotResponseDTO();
        dto.setName(entity.getName());
        dto.setAddress(entity.getAddress());
        dto.setLocation(toGeoJSONPointDTO(entity.getLocation()));

        return dto;
    }

    public void updateEntityFromRequest(DepotRequestDTO dto, Depot entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getAddress() != null) {
            entity.setAddress(dto.getAddress());
        }
        if (dto.getLocation() != null && dto.getLocation().getCoordinates() != null) {
            GeoJSONPoint updatedLocation = toGeoJSONPoint(dto.getLocation());
            if (updatedLocation != null) {
                entity.setLocation(updatedLocation);
            }
        }
    }

    private GeoJSONPoint toGeoJSONPoint(GeoJSONPointDTO dto) {
        if (dto == null) {
            return null;
        }
        double[] coords = dto.getCoordinates();
        if (coords == null || coords.length != 2) {
            return null;
        }

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
