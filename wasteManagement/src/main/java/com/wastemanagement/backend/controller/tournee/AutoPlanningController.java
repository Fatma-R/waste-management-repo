package com.wastemanagement.backend.controller.tournee;

import com.wastemanagement.backend.model.tournee.auto.AutoMode;
import com.wastemanagement.backend.service.tournee.auto.AutoPlanningConfigService;
import com.wastemanagement.backend.service.tournee.auto.AutoPlanningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auto-planning")
public class AutoPlanningController {

    private final AutoPlanningService autoPlanningService;
    private final AutoPlanningConfigService autoPlanningConfigService;

    public AutoPlanningController(AutoPlanningService autoPlanningService,
                                  AutoPlanningConfigService autoPlanningConfigService) {
        this.autoPlanningService = autoPlanningService;
        this.autoPlanningConfigService = autoPlanningConfigService;
    }

    @GetMapping("/mode")
    public ResponseEntity<AutoMode> getMode() {
        return ResponseEntity.ok(autoPlanningConfigService.getAutoMode());
    }

    @PostMapping("/mode/{mode}")
    public ResponseEntity<Void> setMode(@PathVariable AutoMode mode) {
        autoPlanningConfigService.updateAutoMode(mode);
        return ResponseEntity.noContent().build();
    }

    // Manually trigger the daily FULL cycle (respects mode internally)
    @PostMapping("/run/scheduled")
    public ResponseEntity<Void> runScheduledCycle() {
        autoPlanningService.runScheduledCycleCore();
        return ResponseEntity.accepted().build();
    }

    // Manually trigger the emergency loop (respects mode internally)
    @PostMapping("/run/emergency")
    public ResponseEntity<Void> runEmergencyLoop() {
        autoPlanningService.runEmergencyLoopCore();
        return ResponseEntity.accepted().build();
    }
}
