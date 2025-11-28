package com.wastemanagement.backend.dto.collection;


import com.wastemanagement.backend.model.collection.TrashType;
import lombok.Data;

import java.util.List;

@Data
public class BinRequestDTO {
    private String collectionPointId;
    private boolean active;
    private TrashType type;

}
