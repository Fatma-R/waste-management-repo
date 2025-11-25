package com.wastemanagement.backend.dto.collectionPoint;


import com.wastemanagement.backend.model.GeoJSONPoint;
import lombok.Data;

import java.util.List;

@Data
public class CollectionPointRequestDTO {
    private GeoJSONPoint location;
    private boolean active;
    private String adresse;
    private List<String> binIds;
}
