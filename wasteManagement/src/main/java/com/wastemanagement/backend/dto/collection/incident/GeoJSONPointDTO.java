package com.wastemanagement.backend.dto.collection.incident;

import lombok.Data;

@Data
public class GeoJSONPointDTO {
    private double[] coordinates;
}
