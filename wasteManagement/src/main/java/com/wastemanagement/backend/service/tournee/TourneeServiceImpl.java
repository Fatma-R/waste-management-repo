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

    /**
     * Planifie UNE OU PLUSIEURS tournées pour un type de déchet donné.
     * Chaque route renvoyée par VROOM → 1 Tournee, avec un plannedVehicleId.
     */
    @Override
    public List<TourneeResponseDTO> planTourneeWithVroom(TrashType type, double fillThreshold) {
        // 1) Récupérer le dépôt principal
        com.wastemanagement.backend.model.tournee.Depot depot =
                depotService.getMainDepotEntityOrThrow();

        GeoJSONPoint depotLoc = depot.getLocation();
        if (depotLoc == null || depotLoc.getCoordinates() == null) {
            throw new IllegalStateException("Depot location not configured");
        }
        double[] depotCoords = depotLoc.getCoordinates(); // [lon, lat] (GeoJSON)

        // 2) Déterminer les points de collecte à visiter pour ce type
        //    + construire un mapping CP -> volume total en litres
        Map<String, Double> cpIdToVolumeLiters = new HashMap<>();
        List<CollectionPoint> pointsNeedingCollection =
                findCollectionPointsNeedingCollection(type, fillThreshold, cpIdToVolumeLiters);

        if (pointsNeedingCollection.isEmpty()) {
            throw new IllegalStateException(
                    "No collection points need collection for type " + type +
                            " with threshold " + fillThreshold
            );
        }

        // 3) Construire la requête VROOM + mappings :
        //    - jobId -> collectionPointId
        //    - vroomVehicleId -> vehicleId (Mongo)
        Map<Integer, String> jobIdToCollectionPointId = new HashMap<>();
        Map<Integer, String> vroomVehicleIdToVehicleId = new HashMap<>();

        VroomRequest request = buildVroomRequestForPoints(
                depotCoords,
                pointsNeedingCollection,
                cpIdToVolumeLiters,
                jobIdToCollectionPointId,
                vroomVehicleIdToVehicleId
        );

        // 4) Appeler VROOM
        VroomSolution solution = vroomClient.optimize(request);

        if (solution.getRoutes() == null || solution.getRoutes().isEmpty()) {
            throw new IllegalStateException("VROOM returned no routes");
        }

        // 5) Pour CHAQUE route, créer une Tournee
        List<Tournee> tournees = new ArrayList<>();

        for (VroomRoute route : solution.getRoutes()) {
            // Si la route n'a pas de steps (aucun job affecté), on l'ignore
            if (route.getSteps() == null || route.getSteps().isEmpty()) {
                continue;
            }

            // VROOM renvoie l'id du véhicule (celui qu'on lui a donné dans VroomVehicle.setId)
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

        // 6) Sauvegarder toutes les tournées et retourner les DTO
        Iterable<Tournee> savedIterable = tourneeRepository.saveAll(tournees);
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

            String cpId = bin.getCollectionPointId();
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
     * Construit la requête VROOM et remplit les mappings :
     * - jobId -> collectionPointId
     * - vroomVehicleId -> vehicleId (Mongo)
     *
     * Modèle "réaliste" :
     * - Chaque véhicule a une CAPACITÉ en litres (vehicle.capacityVolumeL).
     * - Chaque job a une DEMANDE en litres (volume actuel total des bacs de ce type au CP).
     */
    private VroomRequest buildVroomRequestForPoints(double[] depotCoords,
                                                    List<CollectionPoint> points,
                                                    Map<String, Double> cpIdToVolumeLiters,
                                                    Map<Integer, String> jobIdToCollectionPointId,
                                                    Map<Integer, String> vroomVehicleIdToVehicleId) {

        // --- Vehicles : tous les véhicules en statut AVAILABLE ---
        List<Vehicle> availableVehicles = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);
        if (availableVehicles.isEmpty()) {
            throw new IllegalStateException("No available vehicles for tournee");
        }

        List<VroomVehicle> vroomVehicles = new ArrayList<>();
        int vroomVehicleId = 1;
        for (Vehicle v : availableVehicles) {
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
            throw new IllegalStateException("No AVAILABLE vehicle with valid capacity");
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

        // <-- nouveau : on mémorise le véhicule choisi par VROOM pour cette route
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
}
