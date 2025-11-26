package com.wastemanagement.backend.vroom.dto;

import lombok.Data;

import java.util.List;

@Data
public class VroomRequest {
    private List<VroomVehicle> vehicles;
    private List<VroomJob> jobs;
    private VroomOptions options;
}
