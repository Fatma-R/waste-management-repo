package com.wastemanagement.backend.vroom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VroomRoute {
    private int vehicle;
    private long cost;
    private long duration;
    private long distance;
    private List<VroomStep> steps;
    private String geometry; // encoded polyline when options.g = true
}