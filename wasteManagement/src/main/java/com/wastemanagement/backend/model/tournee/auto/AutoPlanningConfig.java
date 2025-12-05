package com.wastemanagement.backend.model.tournee.auto;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "autoPlanningConfigs")
public class AutoPlanningConfig {

    @Id
    private String id;
    private AutoMode autoMode = AutoMode.OFF;
    private int emergencyScanMinute = 15;
}
