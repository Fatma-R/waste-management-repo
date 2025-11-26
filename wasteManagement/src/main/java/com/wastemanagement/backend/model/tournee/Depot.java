package com.wastemanagement.backend.model.tournee;

import com.wastemanagement.backend.model.GeoJSONPoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "depots")
public class Depot {

    @Id
    private String id;
    private String name;
    private String address;
    private GeoJSONPoint location;
}
