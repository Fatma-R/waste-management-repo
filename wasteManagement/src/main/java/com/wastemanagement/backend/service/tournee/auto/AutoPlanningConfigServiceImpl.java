package com.wastemanagement.backend.service.tournee.auto;

import com.wastemanagement.backend.model.tournee.auto.AutoMode;
import com.wastemanagement.backend.model.tournee.auto.AutoPlanningConfig;
import com.wastemanagement.backend.repository.tournee.AutoPlanningConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AutoPlanningConfigServiceImpl implements AutoPlanningConfigService {

    private static final String SINGLETON_ID = "GLOBAL";
    private final AutoPlanningConfigRepository repo;

    @Override
    public AutoPlanningConfig getOrCreate() {
        Optional<AutoPlanningConfig> config = repo.findById(SINGLETON_ID);
        if (config.isPresent())
            { return config.get(); }
        AutoPlanningConfig newConfig = new AutoPlanningConfig();
        newConfig.setId(SINGLETON_ID);
        newConfig.setAutoMode(AutoMode.OFF);
        return repo.save(newConfig);
    }

    @Override
    public AutoMode getAutoMode() {
        return getOrCreate().getAutoMode();
    }

    @Override
    public AutoMode updateAutoMode(AutoMode autoMode) {
        AutoPlanningConfig config = getOrCreate();
        config.setAutoMode(autoMode);
        repo.save(config);
        return config.getAutoMode();
    }
}
