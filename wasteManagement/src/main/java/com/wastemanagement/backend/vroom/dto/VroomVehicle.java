package com.wastemanagement.backend.vroom.dto;

import lombok.Data;

@Data
public class VroomVehicle {
    private int id;
    private double[] start;    // [lon, lat]
    private double[] end;      // [lon, lat]
    private int[] capacity;    // e.g. [numberOfJobs] or any capacity metric
}
