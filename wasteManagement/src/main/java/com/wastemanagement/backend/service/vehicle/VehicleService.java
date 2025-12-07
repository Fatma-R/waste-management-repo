package com.wastemanagement.backend.service.vehicle;


import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleLocationUpdateDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleResponseDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleRequestDTO;
import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.vehicle.FuelType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface VehicleService {

    VehicleResponseDTO createVehicle(VehicleRequestDTO dto);
    List<VehicleResponseDTO> getAllVehicles(int page, int size);
    VehicleResponseDTO getVehicleById(String id);
    VehicleResponseDTO updateVehicle(String id, VehicleRequestDTO dto);
    boolean deleteVehicle(String id);
    List<VehicleResponseDTO> getVehiclesByStatus(String status);
    List<VehicleResponseDTO> getVehiclesByFuelType(FuelType fuelType);

    List<VehicleResponseDTO> getAvailableVehiclesForTournee(TourneeResponseDTO plannedTournee);

    GeoJSONPoint getCurrentLocation(String vehicleId);

    VehicleResponseDTO updateCurrentLocation(String vehicleId, VehicleLocationUpdateDTO locationUpdate);

    SseEmitter streamLocation(String vehicleId);
}
