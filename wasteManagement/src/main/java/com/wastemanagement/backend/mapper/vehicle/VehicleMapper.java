package com.wastemanagement.backend.mapper.vehicle;

import com.wastemanagement.backend.dto.vehicle.VehicleRequestDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleResponseDTO;
import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.vehicle.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {
    public VehicleResponseDTO toResponseDTO(Vehicle vehicle) {
        if (vehicle == null) return null;

        double[] coords = vehicle.getCurrentLocation() != null
                ? vehicle.getCurrentLocation().getCoordinates()
                : null;

        return new VehicleResponseDTO(
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getCapacityVolumeL(),
                coords,
                vehicle.getFuelType(),
                vehicle.getStatus()
        );
    }
    public Vehicle toEntity(VehicleRequestDTO dto) {
        if (dto == null) return null;

        GeoJSONPoint point = dto.getCoordinates() != null
                ? new GeoJSONPoint(dto.getCoordinates()[0], dto.getCoordinates()[1])
                : null;

        return new Vehicle(
                null,
                dto.getPlateNumber(),
                dto.getCapacityVolumeL(),
                point,
                dto.getFuelType(),
                dto.getStatus()
        );
    }

    public void updateEntity(VehicleRequestDTO dto, Vehicle entity) {
        if (dto.getPlateNumber() != null) entity.setPlateNumber(dto.getPlateNumber());
        if (dto.getCapacityVolumeL() != 0) entity.setCapacityVolumeL(dto.getCapacityVolumeL());
        if (dto.getCoordinates() != null) entity.setCurrentLocation(new GeoJSONPoint(dto.getCoordinates()[0], dto.getCoordinates()[1]));
        if (dto.getFuelType() != null) entity.setFuelType(dto.getFuelType());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
    }
}

