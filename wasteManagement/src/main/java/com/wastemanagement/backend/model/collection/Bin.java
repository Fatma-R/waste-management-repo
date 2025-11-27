package com.wastemanagement.backend.model.collection;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
    }
