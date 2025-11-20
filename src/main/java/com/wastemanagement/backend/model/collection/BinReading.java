package com.wastemanagement.backend.model.collection;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "binReadings")
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
