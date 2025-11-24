package com.wastemanagement.backend.dto.collection;

import lombok.*;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BinReadingResponseDTO {
    private String id;
    private String binId;
    private Date ts;
    private double fillPct;
    private int batteryPct;
    private double temperatureC;
    private int signalDbm;
}
