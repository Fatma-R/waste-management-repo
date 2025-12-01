package com.wastemanagement.backend.service.tournee.auto;

import com.wastemanagement.backend.model.tournee.auto.AutoMode;
import com.wastemanagement.backend.model.tournee.auto.AutoPlanningConfig;

public interface AutoPlanningConfigService {

    public AutoPlanningConfig getOrCreate();
    public AutoMode getAutoMode();
    public AutoMode updateAutoMode(AutoMode autoMode);

}
