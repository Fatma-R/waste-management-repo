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
import com.wastemanagement.backend.model.vehicle.Vehicle;
import com.wastemanagement.backend.model.vehicle.VehicleStatus;
import com.wastemanagement.backend.repository.CollectionPointRepository;
import com.wastemanagement.backend.repository.VehicleRepository;
import com.wastemanagement.backend.repository.collection.BinReadingRepository;
import com.wastemanagement.backend.repository.collection.BinRepository;
import com.wastemanagement.backend.repository.tournee.TourneeRepository;
import com.wastemanagement.backend.vroom.VroomClient;
import com.wastemanagement.backend.vroom.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class TourneeServiceImpl implements TourneeService {

    private final TourneeRepository tourneeRepository;
    private final DepotService depotService;
    private final CollectionPointRepository collectionPointRepository;
    private final BinRepository binRepository;
    private final BinReadingRepository binReadingRepository;
    private final VehicleRepository vehicleRepository;
    private final VroomClient vroomClient;

    /**
     * Approximate capacity of one bin in liters.
     * TODO: replace with real value or per-bin config (e.g. 240L, 360L, 660L)
     */
    private static final double BIN_CAPACITY_L = 660;

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

    // --------------------------------------------------------------------
    // Single-type API + "optional" forced CPs
    // --------------------------------------------------------------------

    /**
     * Single-type API kept for compatibility.
     * It just wraps the single-type planner with no forced collection points.
     */
    @Override
    public List<TourneeResponseDTO> planTourneesWithVroom(TrashType type, double fillThreshold) {
        if (type == null) {
            throw new IllegalArgumentException("TrashType must not be null");
        }
        // "Optional" parameter = null (no forced CPs)
        return planTourneesWithVroom(type, fillThreshold, null);
    }

    /**
     * Single-type planner with an "optional" parameter:
     *
     * @param type                     trash type (required)
     * @param fillThreshold            threshold in percent (ignored if forcedCollectionPointIds is not empty)
     * @param forcedCollectionPointIds if non-null/non-empty, we ONLY plan for these CPs
     *                                 (used for emergency mode, etc.).
     */
    public List<TourneeResponseDTO> planTourneesWithVroom(TrashType type,
                                                          double fillThreshold,
                                                          Set<String> forcedCollectionPointIds) {
        if (type == null) {
            throw new IllegalArgumentException("TrashType must not be null");
        }

        // 1) Depot + coords
        com.wastemanagement.backend.model.tournee.Depot depot =
                depotService.getMainDepotEntityOrThrow();

        GeoJSONPoint depotLoc = depot.getLocation();
        if (depotLoc == null || depotLoc.getCoordinates() == null) {
            throw new IllegalStateException("Depot location not configured");
        }
        double[] depotCoords = depotLoc.getCoordinates(); // [lon, lat] (GeoJSON)

        // 2) Vehicles pool (AVAILABLE + valid capacity)
        List<Vehicle> initialAvailableVehicles =
                vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);

        List<Vehicle> vehiclesPool = initialAvailableVehicles.stream()
                .filter(v -> v.getCapacityVolumeL() > 0)
                .collect(Collectors.toList());

        if (vehiclesPool.isEmpty()) {
            throw new IllegalStateException("No AVAILABLE vehicle with valid capacity");
        }

        // 3) Determine CPs + volumes, either forced list or normal threshold logic
        Map<String, Double> cpIdToVolumeLiters = new HashMap<>();
        List<CollectionPoint> points;

        if (forcedCollectionPointIds != null && !forcedCollectionPointIds.isEmpty()) {
            points = findCollectionPointsForIdsAndType(type, forcedCollectionPointIds, cpIdToVolumeLiters);
        } else {
            points = findCollectionPointsNeedingCollection(type, fillThreshold, cpIdToVolumeLiters);
        }

        if (points.isEmpty()) {
            throw new IllegalStateException(
                    "No collection points need collection for type " + type +
                            " with threshold " + fillThreshold
            );
        }

        // 4) Build VROOM request using the vehicles pool
        Map<Integer, String> jobIdToCollectionPointId = new HashMap<>();
        Map<Integer, String> vroomVehicleIdToVehicleId = new HashMap<>();

        VroomRequest request = buildVroomRequestForPointsWithVehicles(
                depotCoords,
                points,
                cpIdToVolumeLiters,
                jobIdToCollectionPointId,
                vroomVehicleIdToVehicleId,
                vehiclesPool
        );

        // 5) Call VROOM
        VroomSolution solution = vroomClient.optimize(request);

        if (solution.getRoutes() == null || solution.getRoutes().isEmpty()) {
            throw new IllegalStateException("VROOM returned no routes for type " + type);
        }

        // 6) Build Tournees (one per route)
        List<Tournee> tournees = new ArrayList<>();

        for (VroomRoute route : solution.getRoutes()) {
            // Ignore routes with no steps
            if (route.getSteps() == null || route.getSteps().isEmpty()) {
                continue;
            }

            Integer vroomVehicleId = route.getVehicle();
            String plannedVehicleId = null;
            if (vroomVehicleId != null) {
                plannedVehicleId = vroomVehicleIdToVehicleId.get(vroomVehicleId);
            }

            Tournee tournee = buildTourneeFromVroom(
                    type,
                    route,
                    jobIdToCollectionPointId,
                    plannedVehicleId
            );
            tournees.add(tournee);
        }

        if (tournees.isEmpty()) {
            throw new IllegalStateException("VROOM returned only empty routes (no steps)");
        }

        // 7) Save and map to DTOs
        Iterable<Tournee> savedIterable = tourneeRepository.saveAll(tournees);
        List<Tournee> savedList = StreamSupport
                .stream(savedIterable.spliterator(), false)
                .toList();

        return savedList.stream()
                .map(TourneeMapper::toResponse)
                .toList();
    }

    // --------------------------------------------------------------------
    // Multi-type planner (kept as-is, using shared vehicles pool)
    // --------------------------------------------------------------------

    /**
     * New multi-type planner:
     * - Takes a list of TrashType (may contain 1 or more types).
     * - Sorts them by priority: type with the "fullest" bin (highest fillPct) first.
     * - Uses a single pool of AVAILABLE vehicles.
     * - For each type, calls VROOM with the CURRENT pool.
     * - Vehicles used for one type are removed from the pool for the next types.
     * - If one type has no bins/collection points above threshold, it is skipped
     *   (does NOT fail the whole process).
     */
    @Override
    public List<TourneeResponseDTO> planTourneesWithVroom(List<TrashType> types, double fillThreshold) {
        if (types == null || types.isEmpty()) {
            throw new IllegalArgumentException("At least one TrashType is required");
        }

        // 1) Get depot and coordinates ONCE
        com.wastemanagement.backend.model.tournee.Depot depot =
                depotService.getMainDepotEntityOrThrow();

        GeoJSONPoint depotLoc = depot.getLocation();
        if (depotLoc == null || depotLoc.getCoordinates() == null) {
            throw new IllegalStateException("Depot location not configured");
        }
        double[] depotCoords = depotLoc.getCoordinates(); // [lon, lat]

        // 2) Initial vehicles pool (AVAILABLE + valid capacity)
        List<Vehicle> initialAvailableVehicles =
                vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);

        List<Vehicle> vehiclesPool = initialAvailableVehicles.stream()
                .filter(v -> v.getCapacityVolumeL() > 0)
                .collect(Collectors.toList());

        if (vehiclesPool.isEmpty()) {
            throw new IllegalStateException("No AVAILABLE vehicle with valid capacity");
        }

        // 3) Sort trash types by priority = fullest bin (highest fillPct)
        //    Remove duplicates + nulls just in case
        List<TrashType> sortedTypes = types.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        sortedTypes.sort((t1, t2) -> {
            double max2 = computeMaxFillPctForType(t2);
            double max1 = computeMaxFillPctForType(t1);
            // descending: type with highest max fillPct first
            return Double.compare(max2, max1);
        });

        System.out.println("Sorted types by priority (fullest bin first): " + sortedTypes);

        // 4) Plan tours per type using the current vehiclesPool
        List<Tournee> allPlannedTours = new ArrayList<>();

        for (TrashType type : sortedTypes) {
            if (vehiclesPool.isEmpty()) {
                System.out.println("Vehicles pool is empty, stopping planning for remaining types.");
                break;
            }

            System.out.println("Planning tours for type " + type + " with " + vehiclesPool.size() + " vehicles left.");

            // 4.1) Determine collection points that need collection for this type
            Map<String, Double> cpIdToVolumeLiters = new HashMap<>();
            List<CollectionPoint> pointsNeedingCollection =
                    findCollectionPointsNeedingCollection(type, fillThreshold, cpIdToVolumeLiters);

            if (pointsNeedingCollection.isEmpty()) {
                System.out.println("No collection points need collection for type " + type +
                        " with threshold " + fillThreshold + " → skipping this type.");
                continue; // do NOT fail; just move on to next type
            }

            // 4.2) Build VROOM request using CURRENT vehicles pool
            Map<Integer, String> jobIdToCollectionPointId = new HashMap<>();
            Map<Integer, String> vroomVehicleIdToVehicleId = new HashMap<>();

            VroomRequest request = buildVroomRequestForPointsWithVehicles(
                    depotCoords,
                    pointsNeedingCollection,
                    cpIdToVolumeLiters,
                    jobIdToCollectionPointId,
                    vroomVehicleIdToVehicleId,
                    vehiclesPool
            );

            // 4.3) Call VROOM
            VroomSolution solution = vroomClient.optimize(request);

            if (solution.getRoutes() == null || solution.getRoutes().isEmpty()) {
                System.out.println("VROOM returned no routes for type " + type + " → skipping this type.");
                continue;
            }

            // 4.4) Build Tournee for EACH route, track which vehicles were used
            List<Tournee> tourneesForType = new ArrayList<>();
            Set<String> usedVehicleIds = new HashSet<>();

            for (VroomRoute route : solution.getRoutes()) {
                // Ignore routes with no steps
                if (route.getSteps() == null || route.getSteps().isEmpty()) {
                    continue;
                }

                Integer vroomVehicleId = route.getVehicle();
                String plannedVehicleId = null;
                if (vroomVehicleId != null) {
                    plannedVehicleId = vroomVehicleIdToVehicleId.get(vroomVehicleId);
                }

                if (plannedVehicleId != null) {
                    usedVehicleIds.add(plannedVehicleId);
                }

                Tournee tournee = buildTourneeFromVroom(
                        type,
                        route,
                        jobIdToCollectionPointId,
                        plannedVehicleId
                );
                tourneesForType.add(tournee);
            }

            if (tourneesForType.isEmpty()) {
                System.out.println("VROOM returned only empty routes for type " + type + " → skipping.");
                continue;
            }

            // 4.5) Add these tours to global result
            allPlannedTours.addAll(tourneesForType);

            // 4.6) Remove used vehicles from the pool for the next types
            if (!usedVehicleIds.isEmpty()) {
                vehiclesPool = vehiclesPool.stream()
                        .filter(v -> !usedVehicleIds.contains(v.getId()))
                        .collect(Collectors.toList());

                System.out.println("Vehicles used for type " + type + ": " + usedVehicleIds);
                System.out.println("Vehicles remaining in pool: " + vehiclesPool.size());
            }
        }

        // 5) If absolutely nothing could be planned → fail once
        if (allPlannedTours.isEmpty()) {
            throw new IllegalStateException(
                    "No tours could be planned for the requested waste types and threshold."
            );
        }

        // 6) Save all tours and map to DTOs
        Iterable<Tournee> savedIterable = tourneeRepository.saveAll(allPlannedTours);
        List<Tournee> savedList = StreamSupport
                .stream(savedIterable.spliterator(), false)
                .toList();

        return savedList.stream()
                .map(TourneeMapper::toResponse)
                .toList();
    }

    // --------------------------------------------------------------------
    // Helpers internes
    // --------------------------------------------------------------------

    /**
     * Computes the maximum fillPct for a given TrashType across all active bins.
     * Used only to PRIORITIZE types (the one closest to overflowing first).
     * If no bins/readings → returns 0.0.
     */
    private double computeMaxFillPctForType(TrashType type) {
        List<Bin> bins = binRepository.findByActiveTrueAndType(type);
        double maxFill = 0.0;

        for (Bin bin : bins) {
            BinReading latest = binReadingRepository.findTopByBinIdOrderByTsDesc(bin.getId());
            if (latest == null) {
                continue;
            }
            double fill = latest.getFillPct();
            if (fill > maxFill) {
                maxFill = fill;
            }
        }

        System.out.println("Max fillPct for type " + type + " = " + maxFill);
        return maxFill;
    }

    /**
     * Pour un TrashType donné, retourne les CollectionPoint qui doivent être visités.
     * Critère "réaliste" :
     * - On considère tous les bacs actifs de ce type.
     * - Si AU MOINS un bac a un fillPct >= threshold, le CP est sélectionné.
     * - Le volume demandé au CP est la somme des litres de TOUS les bacs de ce type
     *   (pas seulement ceux au-dessus du seuil) → plus proche de la réalité : si le camion passe,
     *   il videra tous les bacs.
     *
     * Remplit aussi cpIdToVolumeLiters (clé = cpId, valeur = volume total en litres).
     */
    private List<CollectionPoint> findCollectionPointsNeedingCollection(TrashType type,
                                                                        double threshold,
                                                                        Map<String, Double> cpIdToVolumeLiters) {
        Set<String> cpAlreadyCovered = getCollectionPointIdsAlreadyCoveredForType(type);
        // a) Tous les bacs actifs de ce type
        List<Bin> bins = binRepository.findByActiveTrueAndType(type);
        System.out.println("Found bins for type " + type + " = " + bins.size());
        if (bins.isEmpty()) {
            return List.of();
        }

        // Pour chaque CP, on va :
        // - accumuler le volume total (tous les bacs de ce type)
        // - savoir s'il y a AU MOINS un bac au-dessus du seuil
        Map<String, Double> cpTotalVolume = new HashMap<>();
        Set<String> cpIdsOverThreshold = new HashSet<>();

        for (Bin bin : bins) {
            String cpId = bin.getCollectionPointId();
            if (cpId == null) { continue; }
            // ignore bins whose CP is already planned/assigned
            if (cpAlreadyCovered.contains(cpId)) {
                System.out.println("Skipping bin " + bin.getId()
                        + " because CP " + cpId + " is already covered (PLANNED/ASSIGNED) for type " + type);
                continue;
            }

            BinReading latest = binReadingRepository.findTopByBinIdOrderByTsDesc(bin.getId());

            System.out.println("Bin " + bin.getId() + " latest reading = "
                    + (latest != null ? latest.getFillPct() : "null"));

            if (latest == null) {
                // Pas de lecture → on ne sait pas, on ignore ce bac pour l’instant
                continue;
            }

            double fillPct = latest.getFillPct();
            // volume de CE bac
            double volumeL = (fillPct / 100.0) * BIN_CAPACITY_L;

            // On additionne TOUS les volumes dans cpTotalVolume
            cpTotalVolume.merge(cpId, volumeL, Double::sum);

            // Si ce bac atteint le seuil, ce CP devient "à traiter"
            if (fillPct >= threshold) {
                cpIdsOverThreshold.add(cpId);
            }
        }

        System.out.println("CP needing collection (over threshold) = " + cpIdsOverThreshold.size());

        if (cpIdsOverThreshold.isEmpty()) {
            return List.of();
        }

        // On remplit la map de sortie : CP -> volume total (en litres)
        cpIdToVolumeLiters.clear();
        for (String cpId : cpIdsOverThreshold) {
            double totalVol = cpTotalVolume.getOrDefault(cpId, 0.0);
            cpIdToVolumeLiters.put(cpId, totalVol);
        }

        // c) Charger les CollectionPoint correspondants
        List<CollectionPoint> allPoints =
                collectionPointRepository.findAllById(cpIdsOverThreshold);

        // d) Optionnel : filtrer sur les points actifs
        return allPoints.stream()
                .filter(CollectionPoint::isActive)
                .collect(Collectors.toList());
    }

    /**
     * Variante de la méthode précédente pour le mode "forced CPs":
     * On ne regarde QUE les CP dont l'id est dans cpIdsFilter, sans seuil.
     */
    private List<CollectionPoint> findCollectionPointsForIdsAndType(TrashType type,
                                                                    Set<String> cpIdsFilter,
                                                                    Map<String, Double> cpIdToVolumeLiters) {
        if (cpIdsFilter == null || cpIdsFilter.isEmpty()) {
            return List.of();
        }

        Set<String> cpAlreadyCovered = getCollectionPointIdsAlreadyCoveredForType(type);
        Set<String> effectiveFilter = cpIdsFilter.stream()
                .filter(cpId -> !cpAlreadyCovered.contains(cpId))
                .collect(Collectors.toSet());

        if (effectiveFilter.isEmpty()) {
            System.out.println("All forced CPs are already covered (PLANNED/ASSIGNED) for type "
                    + type + " → nothing to plan.");
            return List.of();
        }

        List<Bin> bins = binRepository.findByActiveTrueAndType(type);
        System.out.println("Found bins for type " + type + " (forced CP mode) = " + bins.size());
        if (bins.isEmpty()) {
            return List.of();
        }

        Map<String, Double> cpTotalVolume = new HashMap<>();

        for (Bin bin : bins) {
            String cpId = bin.getCollectionPointId();
            if (cpId == null || !cpIdsFilter.contains(cpId)) {
                continue;
            }

            BinReading latest = binReadingRepository.findTopByBinIdOrderByTsDesc(bin.getId());
            System.out.println("Forced mode - bin " + bin.getId() + " latest reading = "
                    + (latest != null ? latest.getFillPct() : "null"));

            if (latest == null) {
                continue;
            }

            double fillPct = latest.getFillPct();
            double volumeL = (fillPct / 100.0) * BIN_CAPACITY_L;
            cpTotalVolume.merge(cpId, volumeL, Double::sum);
        }

        cpIdToVolumeLiters.clear();
        for (String cpId : cpIdsFilter) {
            double totalVol = cpTotalVolume.getOrDefault(cpId, 0.0);
            if (totalVol > 0) {
                cpIdToVolumeLiters.put(cpId, totalVol);
            }
        }

        if (cpIdToVolumeLiters.isEmpty()) {
            return List.of();
        }

        List<CollectionPoint> allPoints =
                collectionPointRepository.findAllById(cpIdToVolumeLiters.keySet());

        return allPoints.stream()
                .filter(CollectionPoint::isActive)
                .collect(Collectors.toList());
    }

    /**
     * Construit la requête VROOM et remplit les mappings :
     * - jobId -> collectionPointId
     * - vroomVehicleId -> vehicleId (Mongo)
     *
     * Modèle "réaliste" :
     * - Chaque véhicule a une CAPACITÉ en litres (vehicle.capacityVolumeL).
     * - Chaque job a une DEMANDE en litres (volume actuel total des bacs de ce type au CP).
     *
     * IMPORTANT: ici on ne lit PAS dans le repository pour les véhicules :
     * on utilise le vehiclesPool fourni (déjà filtré/partagé entre types).
     */
    private VroomRequest buildVroomRequestForPointsWithVehicles(double[] depotCoords,
                                                                List<CollectionPoint> points,
                                                                Map<String, Double> cpIdToVolumeLiters,
                                                                Map<Integer, String> jobIdToCollectionPointId,
                                                                Map<Integer, String> vroomVehicleIdToVehicleId,
                                                                List<Vehicle> vehiclesPool) {

        if (vehiclesPool == null || vehiclesPool.isEmpty()) {
            throw new IllegalStateException("No available vehicles for tournee");
        }

        // --- Vehicles: from the given pool ---
        List<VroomVehicle> vroomVehicles = new ArrayList<>();
        int vroomVehicleId = 1;
        for (Vehicle v : vehiclesPool) {
            double capLiters = v.getCapacityVolumeL(); // champ en litres

            if (capLiters <= 0) {
                System.out.println("Skipping vehicle " + v.getId()
                        + " (invalid capacity: " + capLiters + ")");
                continue;
            }

            VroomVehicle vv = new VroomVehicle();
            vv.setId(vroomVehicleId);

            // On mémorise quel véhicule "réel" se cache derrière cet id VROOM
            vroomVehicleIdToVehicleId.put(vroomVehicleId, v.getId());

            vv.setStart(depotCoords);
            vv.setEnd(depotCoords);
            vv.setCapacity(new int[]{(int) Math.round(capLiters)}); // VROOM travaille en int

            vroomVehicles.add(vv);
            vroomVehicleId++;
        }

        if (vroomVehicles.isEmpty()) {
            throw new IllegalStateException("No AVAILABLE vehicle with valid capacity in pool");
        }

        // --- Jobs : un job par CollectionPoint ---
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

            // Temps de service : simple fonction du nombre de bacs
            int binCount = (cp.getBins() != null) ? cp.getBins().size() : 1;
            job.setService(120L * binCount); // ex: 2 min par bac

            // DEMANDE en litres (réaliste) :
            double volumeLiters = cpIdToVolumeLiters.getOrDefault(cp.getId(), 0.0);
            if (volumeLiters <= 0) {
                throw new IllegalStateException("Inconsistent data: CP " + cp.getId()
                        + " is selected for collection but has volume <= 0");
            }

            int amountLiters = (int) Math.round(volumeLiters);
            job.setAmount(new int[]{amountLiters});

            jobs.add(job);

            // Mapping job -> CP pour reconstruire les RouteSteps
            jobIdToCollectionPointId.put(jobId, cp.getId());
            jobId++;
        }

        VroomOptions options = new VroomOptions();
        options.setG(true); // geometry pour la carte

        VroomRequest req = new VroomRequest();
        req.setVehicles(vroomVehicles);
        req.setJobs(jobs);
        req.setOptions(options);

        return req;
    }

    /**
     * Construit une Tournee + RouteSteps à partir de la route VROOM.
     * Ajoute aussi le plannedVehicleId (id Mongo du véhicule correspondant).
     */
    private Tournee buildTourneeFromVroom(TrashType type,
                                          VroomRoute route,
                                          Map<Integer, String> jobIdToCollectionPointId,
                                          String plannedVehicleId) {

        Tournee tournee = new Tournee();
        tournee.setTourneeType(type);
        tournee.setStatus(TourneeStatus.PLANNED);
        // VROOM distance est généralement en mètres
        tournee.setPlannedKm(route.getDistance() / 1000.0);
        // TODO: calculer plannedCO2 de façon réaliste si besoin
        tournee.setPlannedCO2(0.0);
        tournee.setGeometry(route.getGeometry());
        tournee.setStartedAt(null);
        tournee.setFinishedAt(null);

        // On mémorise le véhicule choisi par VROOM pour cette route
        tournee.setPlannedVehicleId(plannedVehicleId);

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
                rs.setId(null); // Mongo génèrera
                rs.setOrder(order++);
                rs.setStatus(StepStatus.PENDING);
                // Optionnel : on pourrait stocker ici un fillPct prédit / moyen
                rs.setPredictedFillPct(0.0);
                rs.setNotes(null);
                rs.setCollectionPointId(cpId);

                steps.add(rs);
            }
        }

        tournee.setSteps(steps);
        return tournee;
    }

    /**
     * Returns collectionPointIds that already belong to an ASSIGNED tour
     * for the given trash type. Those CPs must NOT be considered again when
     * selecting bins needing collection.
     */
    private Set<String> getCollectionPointIdsAlreadyCoveredForType(TrashType type) {
        Set<String> cpIds = new HashSet<>();
        List<Tournee> assignedTours = tourneeRepository.findByTourneeTypeAndStatus(type, TourneeStatus.IN_PROGRESS);
        for (Tournee t : assignedTours) {
            if (t.getSteps() == null) continue;
            for (RouteStep step : t.getSteps()) {
                if (step.getCollectionPointId() != null) {
                    cpIds.add(step.getCollectionPointId());
                }
            }
        }
        System.out.println("Already covered CPs for type " + type + " (ASSIGNED) = " + cpIds.size());
        return cpIds;
    }

    @Override
    public List<TourneeResponseDTO> findByStatus(TourneeStatus status) {
        List<Tournee> tournees = tourneeRepository.findByStatus(status);
        return tournees.stream()
                .map(TourneeMapper::toResponse)
                .collect(Collectors.toList());
    }
}
