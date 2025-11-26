package com.wastemanagement.backend.vroom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VroomSummary {
    private long cost;
    private long unassigned;
    private long duration;
    private long distance;
}