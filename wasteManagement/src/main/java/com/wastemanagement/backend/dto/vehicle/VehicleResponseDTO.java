package com.wastemanagement.backend.dto.vehicle;


import com.wastemanagement.backend.model.vehicle.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponseDTO {
    private String id;
    private String plateNumber;
    private double capacityVolumeL;
    private double[] coordinates;
    private String fuelType;
    private VehicleStatus status;
}

