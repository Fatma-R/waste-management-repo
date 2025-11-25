package com.wastemanagement.backend.dto.alert;


import com.wastemanagement.backend.model.collection.AlertType;
import lombok.Data;

import java.util.Date;

@Data
public class AlertRequestDTO {
    private String binId;
    private Date ts;
    private AlertType type;
    private double value;
    private boolean cleared;
}

