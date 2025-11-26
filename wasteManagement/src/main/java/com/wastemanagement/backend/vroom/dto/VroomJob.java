package com.wastemanagement.backend.vroom.dto;

import lombok.Data;

@Data
public class VroomJob {
    private int id;
    private int[] amount;      // e.g. [1]
    private long service;      // seconds spent at location, e.g. 300
    private double[] location; // [lon, lat]
}