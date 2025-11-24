package com.wastemanagement.backend.model;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoJSONPoint {
    private String type = "Point";
    private double[] coordinates;

    public GeoJSONPoint(double longitude, double latitude) {

        this.coordinates = new double[]{longitude, latitude};
    }
    public double getLongitude() {
        return coordinates != null && coordinates.length > 0 ? coordinates[0] : 0.0;
    }

    public double getLatitude() {
        return coordinates != null && coordinates.length > 1 ? coordinates[1] : 0.0;
    }


}
