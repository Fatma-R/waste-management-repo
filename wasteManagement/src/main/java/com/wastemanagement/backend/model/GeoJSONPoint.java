package com.wastemanagement.backend.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoJSONPoint {
    private String type = "Point";
    private double[] coordinates;

    public GeoJSONPoint(double longitude, double latitude) {
        this.coordinates = new double[]{longitude, latitude};
    }
}
