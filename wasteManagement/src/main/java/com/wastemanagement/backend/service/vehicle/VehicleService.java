package com.wastemanagement.backend.service.vehicle;


import com.wastemanagement.backend.dto.vehicle.VehicleResponseDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleRequestDTO;

import java.util.List;

public interface VehicleService {

    VehicleResponseDTO createVehicle(VehicleRequestDTO dto);
    List<VehicleResponseDTO> getAllVehicles(int page, int size);
    VehicleResponseDTO getVehicleById(String id);
    VehicleResponseDTO updateVehicle(String id, VehicleRequestDTO dto);
    boolean deleteVehicle(String id);
    List<VehicleResponseDTO> getVehiclesByStatus(String status);
    List<VehicleResponseDTO> getVehiclesByFuelType(String fuelType);
}

