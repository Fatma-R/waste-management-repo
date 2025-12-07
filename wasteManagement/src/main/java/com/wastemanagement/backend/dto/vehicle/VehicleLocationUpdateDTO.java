package com.wastemanagement.backend.dto.vehicle;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Minimal payload to move a vehicle marker without touching other vehicle fields.
 * Accepts standard lat/lng order and will be stored as GeoJSON [lon, lat].
 */
@Data
public class VehicleLocationUpdateDTO {
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
}
