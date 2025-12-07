package com.wastemanagement.backend.mapper;

import com.wastemanagement.backend.dto.vehicle.VehicleRequestDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleResponseDTO;
import com.wastemanagement.backend.model.vehicle.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public VehicleResponseDTO toResponseDTO(Vehicle vehicle) {
        if (vehicle == null) return null;
        return new VehicleResponseDTO(
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getCapacityVolumeL(),
                vehicle.getCurrentLocation(), // GeoJSONPoint directement
                vehicle.getFuelType(),
                vehicle.getStatus(),
                vehicle.isBusy()
        );
    }

    public Vehicle toEntity(VehicleRequestDTO dto) {
        if (dto == null) return null;
        return new Vehicle(
                null,
                dto.getPlateNumber(),
                dto.getCapacityVolumeL(),
                dto.getCurrentLocation(),
                dto.getFuelType(),
                dto.getStatus(),
                dto.getBusy() != null ? dto.getBusy() : false
        );
    }

    public void updateEntity(VehicleRequestDTO dto, Vehicle entity) {
        if (dto.getPlateNumber() != null) {
            entity.setPlateNumber(dto.getPlateNumber());
        }
        if (dto.getCapacityVolumeL() != 0) {
            entity.setCapacityVolumeL(dto.getCapacityVolumeL());
        }
        if (dto.getCurrentLocation() != null) { // même nom que dans le DTO
            entity.setCurrentLocation(dto.getCurrentLocation());
        }
        if (dto.getFuelType() != null) {
            entity.setFuelType(dto.getFuelType());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getBusy() != null) {
            entity.setBusy(dto.getBusy());
        }
    }
}
