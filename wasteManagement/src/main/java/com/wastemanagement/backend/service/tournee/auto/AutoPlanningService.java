package com.wastemanagement.backend.service.tournee.auto;

import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.model.collection.TrashType;
import com.wastemanagement.backend.model.tournee.Tournee;
import com.wastemanagement.backend.model.tournee.auto.AutoMode;
import com.wastemanagement.backend.model.tournee.auto.BinSnapshot;
import com.wastemanagement.backend.service.tournee.TourneeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoPlanningService {

    private final AutoPlanningConfigService autoModeService;
    private final BinSnapshotService binSnapshotService;
    private final TourneeService tourneeService;

    /**
     * Emergency loop (every 15 minutes).
     * - OFF          -> do nothing
     * - EMERGENCY_ONLY or FULL -> plan emergency tours
     */
    @Scheduled(fixedDelay = 15 * 60 * 1000L)
    public void runEmergencyLoop() {
        AutoMode mode = autoModeService.getAutoMode();
        if (mode == AutoMode.OFF) {
            return;
        }

        log.info("AutoPlanning emergency loop, mode={}", mode);

        var emergencies = binSnapshotService.getEmergencySnapshots();
        if (emergencies.isEmpty()) {
            log.info("No emergency bins at this time.");
            return;
        }

        // group emergency bins by TrashType
        Map<TrashType, Set<String>> cpIdsByType = emergencies.stream()
                .collect(Collectors.groupingBy(
                        BinSnapshot::getTrashType,
                        Collectors.mapping(BinSnapshot::getCollectionPointId, Collectors.toSet())
                ));

        cpIdsByType.forEach((type, cpIds) -> {
            if (cpIds == null || cpIds.isEmpty()) {
                log.info("No emergency CPs for type={}, skipping", type);
                return;
            }

            log.info("Emergency planning for type={}, cpCount={}", type, cpIds.size());

            try {
                // fillThreshold is ignored in forced-CP mode, but we pass 0.0 for clarity
                var tours = tourneeService.planTourneesWithVroom(type, 0.0, cpIds);
                log.info("Planned {} emergency tours for type {}", tours.size(), type);
            } catch (Exception e) {
                log.error("Error planning emergency tours for type {}", type, e);
            }
        });

    }

    /**
     * Scheduled "full" cycles, e.g. once per day at 3am.
     * - OFF / EMERGENCY_ONLY -> skip
     * - FULL                 -> run your normal daily planning
     * Second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void runScheduledCycle() {
        AutoMode mode = autoModeService.getAutoMode();
        if (mode != AutoMode.FULL) {
            return;
        }

        log.info("Running FULL scheduled cycle for all trash types");
        List<TrashType> allTypes = List.of(TrashType.PLASTIC, TrashType.ORGANIC,
                TrashType.GLASS, TrashType.PAPER);

        try {
            while (true) {
                List<TourneeResponseDTO> planned = tourneeService.planTourneesWithVroom(allTypes, 80.0);
                if (planned == null || planned.isEmpty()) {
                    log.info("No more bins over threshold; stopping scheduled cycle.");
                    break;
                }
                log.info("Planned {} tours; re-running to catch remaining bins", planned.size());
            }
        } catch (Exception e) {
            log.error("Error in FULL scheduled cycle", e);
        }
    }

}
