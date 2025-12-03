package com.wastemanagement.backend.service.tournee.auto;

import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.model.collection.BinReading;
import com.wastemanagement.backend.model.collection.TrashType;
import com.wastemanagement.backend.model.tournee.auto.BinSnapshot;
import com.wastemanagement.backend.repository.collection.BinReadingRepository;
import com.wastemanagement.backend.repository.collection.BinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinSnapshotService {
    private final BinRepository binRepository;
    private final BinReadingRepository binReadingRepository;

    private final Map<String, BinSnapshot> snapshots = new ConcurrentHashMap<>();

    public Collection<BinSnapshot> getAllSnapshots(){
        return snapshots.values();
    }

    public Collection<BinSnapshot> getEmergencySnapshots(){
        return snapshots.values().stream()
                .filter(BinSnapshot::isEmergency)
                .toList();
    }

    @Scheduled(fixedDelay = 15 * 60 * 1000L)
    public void refreshSnapshots() {
        log.info("Refreshing bin snapshots for auto planning...");

        var allBins = binRepository.findByActiveTrue(); // or whatever you have

        Map<String, BinSnapshot> newMap = new ConcurrentHashMap<>();
        Instant now = Instant.now();

        for (Bin bin : (List<Bin>) allBins) {
            BinReading latest = binReadingRepository.findTopByBinIdOrderByTsDesc(bin.getId());
            if (latest == null) {
                continue;
            }

            double fillPct = latest.getFillPct();
            Instant lastCollectedAt = bin.getLastCollectedAt(); // add this field to Bin

            EmergencyEval eval = evaluateEmergency(bin.getType(), fillPct, lastCollectedAt, now);

            BinSnapshot snap = BinSnapshot.builder()
                    .binId(bin.getId())
                    .collectionPointId(bin.getCollectionPointId())
                    .trashType(bin.getType())
                    .fillPct(fillPct)
                    .lastCollectedAt(lastCollectedAt)
                    .emergency(eval.isEmergency)
                    .emergencyReason(eval.reason)
                    .build();

            newMap.put(bin.getId(), snap);
        }

        snapshots.clear();
        snapshots.putAll(newMap);
        log.info("Bin snapshots refreshed, total={}, emergencies={}",
                snapshots.size(),
                snapshots.values().stream().filter(BinSnapshot::isEmergency).count());
    }

    private record EmergencyEval(boolean isEmergency, String reason) {}

    private EmergencyEval evaluateEmergency(TrashType type, double fillPct, Instant lastCollectedAt, Instant now) {
        // rule 1: any bin > 95%
        if (fillPct >= 95.0) {
            return new EmergencyEval(true, "FILL>95%");
        }

        if (lastCollectedAt == null) {
            return new EmergencyEval(false, "");
        }

        long hours = Duration.between(lastCollectedAt, now).toHours();

        // rule 2: organic > 40% and uncollected > 48h
        if (type == TrashType.ORGANIC && fillPct > 40.0 && hours >= 48) {
            return new EmergencyEval(true, "ORGANIC>40%&>48h");
        }

        // rule 3: organic any level, uncollected > 72h
        if (type == TrashType.ORGANIC && hours >= 72) {
            return new EmergencyEval(true, "ORGANIC>72h");
        }

        return new EmergencyEval(false, "");
    }
}