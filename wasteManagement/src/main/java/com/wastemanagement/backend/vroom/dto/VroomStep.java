package com.wastemanagement.backend.vroom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VroomStep {
    private String type;       // "start", "job", "end"
    private Integer job;       // job id when type == "job"
    private double[] location; // [lon, lat]
    private long arrival;
    private long duration;
    private long distance;
}
