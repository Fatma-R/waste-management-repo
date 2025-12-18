package com.wastemanagement.backend.model.collection;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "binReadings")
@CompoundIndexes({
        // Optimizes lookups of recent readings per bin
        // (findTopByBinIdOrderByTsDesc, range queries by ts)
        @CompoundIndex(name = "bin_ts_desc_idx",
                def = "{ 'binId': 1, 'ts': -1 }")
})
public class BinReading {
    @Id
    private String id;
    private String binId;
    private Date ts;
    private double fillPct;
    private int batteryPct;
    private double temperatureC;
    private int signalDbm;
}
