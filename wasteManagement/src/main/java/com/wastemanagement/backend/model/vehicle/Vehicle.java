package com.wastemanagement.backend.model.vehicle;

import com.wastemanagement.backend.model.GeoJSONPoint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vehicles")
@CompoundIndexes({
        // Optimizes vehicle assignment queries
        // filtering by availability
        @CompoundIndex(name = "status_busy_idx",
                def = "{ 'status': 1, 'busy': 1 }")
})
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
