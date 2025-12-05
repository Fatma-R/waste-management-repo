package com.wastemanagement.backend.dto.vehicle;

import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.vehicle.FuelType;
import com.wastemanagement.backend.model.vehicle.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRequestDTO {
    private String plateNumber;
    private double capacityVolumeL;
    private GeoJSONPoint currentLocation;
    private FuelType fuelType;
    private VehicleStatus status;
    private Boolean busy;
}
