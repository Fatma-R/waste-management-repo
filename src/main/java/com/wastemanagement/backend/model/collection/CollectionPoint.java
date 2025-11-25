package com.wastemanagement.backend.model.collection;

import com.wastemanagement.backend.model.GeoJSONPoint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "collectionPoints")
public class CollectionPoint {
    @Id
    private String id;
    private GeoJSONPoint location;
    private boolean active;
    private String adresse;
    private List<Bin> bins;
}
