package com.wastemanagement.backend.model.tournee.auto;

import com.wastemanagement.backend.model.collection.TrashType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinSnapshot {
    private String binId;
    private String collectionPointId;
    private TrashType trashType;
    private double fillPct;
    private Instant lastCollectedAt;
    private boolean emergency;
    private String emergencyReason;
}
