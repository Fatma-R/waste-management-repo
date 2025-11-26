package com.wastemanagement.backend.vroom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VroomSolution {
    private int code;                  // 0 = ok
    private String error;              // message if code != 0
    private VroomSummary summary;
    private List<Object> unassigned;   // ignore details for now
    private List<VroomRoute> routes;
}