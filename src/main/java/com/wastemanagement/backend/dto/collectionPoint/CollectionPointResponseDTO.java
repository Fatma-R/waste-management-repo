package com.wastemanagement.backend.dto.collectionPoint;



import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.collection.Bin;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CollectionPointResponseDTO {
    private String id;
    private GeoJSONPoint location;
    private boolean active;
    private String adresse;
    private List<Bin> bins;
}
