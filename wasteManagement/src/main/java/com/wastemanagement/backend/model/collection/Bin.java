package com.wastemanagement.backend.model.collection;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bins")
public class Bin {
    @Id
    private String id;
    private String collectionPointId;
    private boolean active;
    private TrashType type;
    // new field. Need to track truck location to fill it accurately
    // or declare a custom method for when the bin level suddenly drops
    // or estimate it based on the last time it was under 10% (easiest)
    private Instant lastCollectedAt;
    }
