package com.wastemanagement.backend.dto.alert;


import com.wastemanagement.backend.model.collection.AlertType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequestDTO {
    private Date ts;
    private String binId;
    private AlertType type;
    private double value;
    private boolean cleared;
}

