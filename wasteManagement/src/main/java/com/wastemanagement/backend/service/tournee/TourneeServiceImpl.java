package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.mapper.tournee.TourneeMapper;
import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.model.collection.BinReading;
import com.wastemanagement.backend.model.collection.CollectionPoint;
import com.wastemanagement.backend.model.collection.TrashType;
import com.wastemanagement.backend.model.tournee.RouteStep;
import com.wastemanagement.backend.model.tournee.StepStatus;
import com.wastemanagement.backend.model.tournee.Tournee;
import com.wastemanagement.backend.model.tournee.TourneeStatus;
import com.wastemanagement.backend.repository.CollectionPointRepository;
import com.wastemanagement.backend.repository.collection.BinReadingRepository;
import com.wastemanagement.backend.repository.collection.BinRepository;
import com.wastemanagement.backend.repository.tournee.TourneeRepository;
import com.wastemanagement.backend.vroom.VroomClient;
import com.wastemanagement.backend.vroom.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourneeServiceImpl implements TourneeService {

    private final TourneeRepository tourneeRepository;
    private final DepotService depotService;
    private final CollectionPointRepository collectionPointRepository;
    private final BinRepository binRepository;
    private final BinReadingRepository binReadingRepository;
    private final VroomClient vroomClient;

    @Override
    public TourneeResponseDTO createTournee(TourneeRequestDTO dto) {
        Tournee tournee = TourneeMapper.toEntity(dto);
        Tournee saved = tourneeRepository.save(tournee);
        return TourneeMapper.toResponse(saved);
    }

    @Override
    public TourneeResponseDTO updateTournee(String id, TourneeRequestDTO dto) {
        Tournee existing = tourneeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournee not found"));
        TourneeMapper.updateEntity(existing, dto);
        Tournee saved = tourneeRepository.save(existing);
        return TourneeMapper.toResponse(saved);
    }

    @Override
    public TourneeResponseDTO getTourneeById(String id) {
        Tournee tournee = tourneeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournee not found"));
        return TourneeMapper.toResponse(tournee);
    }

    @Override
    public List<TourneeResponseDTO> getAllTournees() {
        return ((List<Tournee>) tourneeRepository.findAll())
                .stream()
                .map(TourneeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteTournee(String id) {
        tourneeRepository.deleteById(id);
    }

    @Override
    public TourneeResponseDTO planTourneeWithVroom(TrashType type, double fillThreshold) {
        // 1) Récupérer le dépôt principal
        com.wastemanagement.backend.model.tournee.Depot depot =
                depotService.getMainDepotEntityOrThrow();

        GeoJSONPoint depotLoc = depot.getLocation();
        if (depotLoc == null || depotLoc.getCoordinates() == null) {
            throw new IllegalStateException("Depot location not configured");
        }
        double[] depotCoords = depotLoc.getCoordinates(); // [lon, lat]

        // 2) Déterminer les points de collecte à visiter pour ce type
        List<CollectionPoint> pointsNeedingCollection =
                findCollectionPointsNeedingCollection(type, fillThreshold);

        if (pointsNeedingCollection.isEmpty()) {
            throw new IllegalStateException(
                    "No collection points need collection for type " + type +
                            " with threshold " + fillThreshold
            );
        }

        // 3) Construire la requête VROOM + mapping jobId -> collectionPointId
        Map<Integer, String> jobIdToCollectionPointId = new HashMap<>();
        VroomRequest request = buildVroomRequestForPoints(
                depotCoords,
                pointsNeedingCollection,
                jobIdToCollectionPointId
        );

        // 4) Appeler VROOM
        VroomSolution solution = vroomClient.optimize(request);

        if (solution.getRoutes() == null || solution.getRoutes().isEmpty()) {
            throw new IllegalStateException("VROOM returned no routes");
        }

        VroomRoute mainRoute = solution.getRoutes().get(0); // pour l’instant : un seul véhicule

        // 5) Créer l’entité Tournee + RouteSteps
        Tournee tournee = buildTourneeFromVroom(type, mainRoute, jobIdToCollectionPointId);

        // 6) Sauvegarder + retourner le DTO
        Tournee saved = tourneeRepository.save(tournee);
        return TourneeMapper.toResponse(saved);
    }

    // --------------------------------------------------------------------
    // Helpers internes
    // --------------------------------------------------------------------

    /**
     * Pour un TrashType donné, retourne les CollectionPoint qui ont au moins
     * une poubelle (Bin) de ce type avec un dernier fillPct >= threshold.
     */
    private List<CollectionPoint> findCollectionPointsNeedingCollection(TrashType type,
                                                                        double threshold) {

        // a) Tous les bacs actifs de ce type
        List<Bin> bins = binRepository.findByActiveTrueAndType(type);
        System.out.println("Found bins for type " + type + " = " + bins.size());
        if (bins.isEmpty()) {
            return List.of();
        }

        // b) Pour chaque bac, regarder la dernière lecture
        Set<String> collectionPointIdsNeedingCollection = new HashSet<>();

        for (Bin bin : bins) {
            BinReading latest = binReadingRepository
                    .findTopByBinIdOrderByTsDesc(bin.getId());
            System.out.println("Bin " + bin.getId() + " latest reading = " +
                    (latest != null ? latest.getFillPct() : "null"));
            if (latest != null && latest.getFillPct() >= threshold) {
                collectionPointIdsNeedingCollection.add(bin.getCollectionPointId());
            }
        }

        System.out.println("CP needing collection = " + collectionPointIdsNeedingCollection.size());

        if (collectionPointIdsNeedingCollection.isEmpty()) {
            return List.of();
        }

        // c) Charger les CollectionPoint correspondants
        List<CollectionPoint> allPoints =
                collectionPointRepository.findAllById(collectionPointIdsNeedingCollection);

        // d) Optionnel : filtrer sur les points actifs
        return allPoints.stream()
                .filter(CollectionPoint::isActive)
                .collect(Collectors.toList());
    }

    /**
     * Construit la requête VROOM et remplit un mapping jobId -> collectionPointId.
     */
    private VroomRequest buildVroomRequestForPoints(double[] depotCoords,
                                                    List<CollectionPoint> points,
                                                    Map<Integer, String> jobIdToCollectionPointId) {

        // --- Vehicles (pour l’instant : 1 véhicule) ---
        VroomVehicle vehicle = new VroomVehicle();
        vehicle.setId(1);
        vehicle.setStart(depotCoords);
        vehicle.setEnd(depotCoords);
        vehicle.setCapacity(new int[]{points.size()}); // peut gérer tous les jobs

        // --- Jobs ---
        List<VroomJob> jobs = new ArrayList<>();
        int jobId = 1;

        for (CollectionPoint cp : points) {
            GeoJSONPoint loc = cp.getLocation();
            if (loc == null || loc.getCoordinates() == null) {
                continue;
            }

            double[] coords = loc.getCoordinates(); // [lon, lat]

            VroomJob job = new VroomJob();
            job.setId(jobId);
            job.setLocation(coords);

            int binCount = (cp.getBins() != null) ? cp.getBins().size() : 1;
            job.setService(120L * binCount);        // ex: 2 minutes par bac
            job.setAmount(new int[]{binCount});     // charge = nb de bacs

            jobs.add(job);

            // On garde la correspondance job -> collectionPointId
            jobIdToCollectionPointId.put(jobId, cp.getId());
            jobId++;
        }

        VroomOptions options = new VroomOptions();
        options.setG(true); // geometry pour la carte

        VroomRequest req = new VroomRequest();
        req.setVehicles(List.of(vehicle));
        req.setJobs(jobs);
        req.setOptions(options);

        return req;
    }

    /**
     * Construit une Tournee + RouteSteps à partir de la route VROOM.
     */
    private Tournee buildTourneeFromVroom(TrashType type,
                                          VroomRoute route,
                                          Map<Integer, String> jobIdToCollectionPointId) {

        Tournee tournee = new Tournee();
        tournee.setTourneeType(type);
        tournee.setStatus(TourneeStatus.PLANNED); // ajuste selon ton enum
        // VROOM distance est souvent en mètres → km approximatifs
        tournee.setPlannedKm(route.getDistance() / 1000.0);
        // TODO: calculer plannedCO2 si tu veux, sinon 0 pour le moment
        tournee.setPlannedCO2(0.0);
        tournee.setGeometry(route.getGeometry());
        tournee.setStartedAt(null);
        tournee.setFinishedAt(null);

        List<RouteStep> steps = new ArrayList<>();
        int order = 0;

        if (route.getSteps() != null) {
            for (VroomStep step : route.getSteps()) {
                if (!"job".equalsIgnoreCase(step.getType())) {
                    continue; // on ignore start/end pour les RouteStep
                }

                Integer jobId = step.getJob();
                if (jobId == null) {
                    continue;
                }

                String cpId = jobIdToCollectionPointId.get(jobId);
                if (cpId == null) {
                    continue;
                }

                RouteStep rs = new RouteStep();
                rs.setId(null); // laisser Mongo générer
                rs.setOrder(order++);
                rs.setStatus(StepStatus.PENDING);  // ajuste selon ton enum
                rs.setPredictedFillPct(0.0);       // optional: tu peux mettre le fillPct max du CP
                rs.setNotes(null);
                rs.setCollectionPointId(cpId);

                steps.add(rs);
            }
        }

        tournee.setSteps(steps);
        return tournee;
    }
}
