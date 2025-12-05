package com.wastemanagement.backend.model.vehicle;

import com.wastemanagement.backend.model.GeoJSONPoint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vehicles")
public class Vehicle {
    @Id
    private String id;
    private String plateNumber;
    private double capacityVolumeL;
    private GeoJSONPoint currentLocation;
    private FuelType fuelType;
    private VehicleStatus status;
    private boolean busy;
}
