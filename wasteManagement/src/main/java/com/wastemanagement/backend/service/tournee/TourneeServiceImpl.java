package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.mapper.tournee.TourneeMapper;
import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.collection.Bin;
import com.wastemanagement.backend.model.collection.BinReading;
import com.wastemanagement.backend.model.collection.CollectionPoint;
import com.wastemanagement.backend.model.collection.TrashType;
import com.wastemanagement.backend.model.tournee.*;
import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.model.user.EmployeeStatus;
import com.wastemanagement.backend.model.vehicle.Vehicle;
import com.wastemanagement.backend.model.vehicle.VehicleStatus;
import com.wastemanagement.backend.repository.collection.BinReadingRepository;
import com.wastemanagement.backend.repository.collection.BinRepository;
import com.wastemanagement.backend.repository.CollectionPointRepository;
import com.wastemanagement.backend.repository.tournee.TourneeAssignmentRepository;
import com.wastemanagement.backend.repository.tournee.TourneeRepository;
import com.wastemanagement.backend.repository.VehicleRepository;
import com.wastemanagement.backend.repository.user.EmployeeRepository;
import com.wastemanagement.backend.vroom.VroomClient;
import com.wastemanagement.backend.vroom.dto.VroomJob;
import com.wastemanagement.backend.vroom.dto.VroomOptions;
import com.wastemanagement.backend.vroom.dto.VroomRequest;
import com.wastemanagement.backend.vroom.dto.VroomRoute;
import com.wastemanagement.backend.vroom.dto.VroomSolution;
import com.wastemanagement.backend.vroom.dto.VroomStep;
import com.wastemanagement.backend.vroom.dto.VroomVehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class TourneeServiceImpl implements TourneeService {

    /**
     * Approximate capacity of one bin in liters.
     * TODO: replace with real value or per-bin config (e.g. 240L, 360L, 660L)
     */
    private static final double BIN_CAPACITY_L = 660;

    private final TourneeRepository tourneeRepository;
    private final CollectionPointRepository collectionPointRepository;
    private final BinRepository binRepository;
    private final BinReadingRepository binReadingRepository;
    private final VehicleRepository vehicleRepository;
    private final DepotService depotService;
    private final VroomClient vroomClient;
    private final EmployeeRepository employeeRepository;
    private final TourneeAssignmentRepository tourneeAssignmentRepository;

    @Override
    public TourneeResponseDTO createTournee(TourneeRequestDTO dto) {
        Tournee entity = TourneeMapper.toEntity(dto);
        entity = tourneeRepository.save(entity);
        return TourneeMapper.toResponse(entity);
    }

    @Override
    public TourneeResponseDTO updateTournee(String id, TourneeRequestDTO dto) {
        Optional<Tournee> optional = tourneeRepository.findById(id);
        if (optional.isEmpty()) {
            return null;
        }
        Tournee existing = optional.get();
        TourneeMapper.updateEntity(existing, dto);
        return TourneeMapper.toResponse(tourneeRepository.save(existing));
    }

    @Override
    public TourneeResponseDTO getTourneeById(String id) {
        return tourneeRepository.findById(id)
                .map(TourneeMapper::toResponse)
                .orElse(null);
    }

    @Override
    public List<TourneeResponseDTO> getAllTournees() {
        return StreamSupport.stream(tourneeRepository.findAll().spliterator(), false)
                .map(TourneeMapper::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public void deleteTournee(String id) {
        // 1. Load the tournee or fail
        Tournee tournee = tourneeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tournee not found: " + id));

        // 2. Load assignments for this tournee
        List<TourneeAssignment> assignments =
                tourneeAssignmentRepository.findByTourneeId(id);

        // 3. Free vehicle if any
        String vehicleId = tournee.getPlannedVehicleId();
        if (vehicleId != null) {
            vehicleRepository.findById(vehicleId).ifPresent(vehicle -> {
                vehicle.setBusy(false);
                vehicleRepository.save(vehicle);
            });
        }

        // 4. Free employees if any assignments
        if (!assignments.isEmpty()) {
            List<String> employeeIds = assignments.stream()
                    .map(TourneeAssignment::getEmployeeId)
                    .distinct()
                    .toList();

            List<Employee> employees = (List<Employee>) employeeRepository.findAllById(employeeIds);
            for (Employee e : employees) {
                e.setStatus(EmployeeStatus.FREE); // your enum
            }
            employeeRepository.saveAll(employees);
        }

        // 5. Delete assignments
        if (!assignments.isEmpty()) {
            tourneeAssignmentRepository.deleteAll(assignments);
        }

        // 6. Delete tournee
        tourneeRepository.delete(tournee);
    }


    // --------------------------------------------------------------------
    // Single-type API + "optional" forced CPs
    // --------------------------------------------------------------------
    @Override
    public List<TourneeResponseDTO> planTourneesWithVroom(TrashType type, double fillThreshold) {
        return planTourneesWithVroom(type, fillThreshold, null);
    }

    @Override
    public List<TourneeResponseDTO> planTourneesWithVroom(TrashType type,
                                                          double fillThreshold,
                                                          Set<String> forcedCollectionPointIds) {
        if (type == null) {
            throw new IllegalArgumentException("TrashType must not be null");
        }

        com.wastemanagement.backend.model.tournee.Depot depot =
                depotService.getMainDepotEntityOrThrow();

        GeoJSONPoint depotLoc = depot.getLocation();
        if (depotLoc == null || depotLoc.getCoordinates() == null) {
            throw new IllegalStateException("Depot location not configured");
        }
        double[] depotCoords = depotLoc.getCoordinates(); // [lon, lat]

        List<Vehicle> vehiclesPool =
                vehicleRepository.findByStatusAndBusyFalse(VehicleStatus.AVAILABLE).stream()
                        .filter(v -> v.getCapacityVolumeL() > 0)
                        .toList();

        if (vehiclesPool.isEmpty()) {
            throw new IllegalStateException("No AVAILABLE vehicle with valid capacity");
        }

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

        VroomSolution solution = vroomClient.optimize(request);
        if (solution.getRoutes() == null || solution.getRoutes().isEmpty()) {
            throw new IllegalStateException("VROOM returned no routes for type " + type);
        }

        List<Tournee> tournees = new ArrayList<>();
        Set<String> usedVehicleIds = new HashSet<>();

        for (VroomRoute route : solution.getRoutes()) {
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
            tournees.add(tournee);
        }

        if (tournees.isEmpty()) {
            throw new IllegalStateException("VROOM returned only empty routes (no steps)");
        }

        Iterable<Tournee> savedIterable = tourneeRepository.saveAll(tournees);
        List<Tournee> savedList = StreamSupport
                .stream(savedIterable.spliterator(), false)
                .toList();

        markVehiclesBusy(
                savedList.stream()
                        .map(Tournee::getPlannedVehicleId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );

        return savedList.stream()
                .map(TourneeMapper::toResponse)
                .toList();
    }

    // --------------------------------------------------------------------
    // Multi-type planner (shared vehicles pool)
    // --------------------------------------------------------------------
    @Override
    public List<TourneeResponseDTO> planTourneesWithVroom(List<TrashType> types, double fillThreshold) {
        if (types == null || types.isEmpty()) {
            throw new IllegalArgumentException("At least one TrashType is required");
        }

        com.wastemanagement.backend.model.tournee.Depot depot =
                depotService.getMainDepotEntityOrThrow();

        GeoJSONPoint depotLoc = depot.getLocation();
        if (depotLoc == null || depotLoc.getCoordinates() == null) {
            throw new IllegalStateException("Depot location not configured");
        }
        double[] depotCoords = depotLoc.getCoordinates(); // [lon, lat]

        List<Vehicle> vehiclesPool =
                vehicleRepository.findByStatusAndBusyFalse(VehicleStatus.AVAILABLE).stream()
                        .filter(v -> v.getCapacityVolumeL() > 0)
                        .collect(Collectors.toCollection(ArrayList::new));

        if (vehiclesPool.isEmpty()) {
            throw new IllegalStateException("No AVAILABLE vehicle with valid capacity");
        }

        List<TrashType> sortedTypes = types.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted((t1, t2) -> {
                    double max2 = computeMaxFillPctForType(t2);
                    double max1 = computeMaxFillPctForType(t1);
                    return Double.compare(max2, max1);
                })
                .toList();

        List<Tournee> allPlannedTours = new ArrayList<>();

        for (TrashType type : sortedTypes) {
            if (vehiclesPool.isEmpty()) {
                break;
            }

            Map<String, Double> cpIdToVolumeLiters = new HashMap<>();
            List<CollectionPoint> pointsNeedingCollection =
                    findCollectionPointsNeedingCollection(type, fillThreshold, cpIdToVolumeLiters);

            if (pointsNeedingCollection.isEmpty()) {
                continue;
            }

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

            VroomSolution solution = vroomClient.optimize(request);
            if (solution.getRoutes() == null || solution.getRoutes().isEmpty()) {
                continue;
            }

            List<Tournee> tourneesForType = new ArrayList<>();
            Set<String> usedVehicleIds = new HashSet<>();

            for (VroomRoute route : solution.getRoutes()) {
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
                continue;
            }

            allPlannedTours.addAll(tourneesForType);

            if (!usedVehicleIds.isEmpty()) {
                vehiclesPool = vehiclesPool.stream()
                        .filter(v -> !usedVehicleIds.contains(v.getId()))
                        .collect(Collectors.toCollection(ArrayList::new));
            }
        }

        if (allPlannedTours.isEmpty()) {
            throw new IllegalStateException(
                    "No tours could be planned for the requested waste types and threshold."
            );
        }

        Iterable<Tournee> savedIterable = tourneeRepository.saveAll(allPlannedTours);
        List<Tournee> savedList = StreamSupport
                .stream(savedIterable.spliterator(), false)
                .toList();

        markVehiclesBusy(
                savedList.stream()
                        .map(Tournee::getPlannedVehicleId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );

        return savedList.stream()
                .map(TourneeMapper::toResponse)
                .toList();
    }

    // --------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------
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

        return maxFill;
    }

    private List<CollectionPoint> findCollectionPointsNeedingCollection(TrashType type,
                                                                        double threshold,
                                                                        Map<String, Double> cpIdToVolumeLiters) {

        Set<String> cpAlreadyCovered = getCollectionPointIdsAlreadyCoveredForType(type);
        List<Bin> bins = binRepository.findByActiveTrueAndType(type);
        if (bins.isEmpty()) {
            return List.of();
        }

        Map<String, Double> cpTotalVolume = new HashMap<>();

        for (Bin bin : bins) {
            String cpId = bin.getCollectionPointId();
            if (cpId == null) {
                continue;
            }
            if (cpAlreadyCovered.contains(cpId)) {
                continue;
            }

            BinReading latest = binReadingRepository.findTopByBinIdOrderByTsDesc(bin.getId());
            if (latest == null) {
                continue;
            }

            double fillPct = latest.getFillPct();
            if (fillPct < threshold) {
                continue;
            }

            double volumeL = (fillPct / 100.0) * BIN_CAPACITY_L;
            cpTotalVolume.merge(cpId, volumeL, Double::sum);
        }

        cpIdToVolumeLiters.clear();
        cpIdToVolumeLiters.putAll(cpTotalVolume);

        if (cpTotalVolume.isEmpty()) {
            return List.of();
        }

        List<CollectionPoint> allPoints =
                collectionPointRepository.findAllById(cpTotalVolume.keySet());

        return allPoints.stream()
                .filter(CollectionPoint::isActive)
                .collect(Collectors.toList());
    }

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
            return List.of();
        }

        List<Bin> bins = binRepository.findByActiveTrueAndType(type);
        if (bins.isEmpty()) {
            return List.of();
        }

        Map<String, Double> cpTotalVolume = new HashMap<>();

        for (Bin bin : bins) {
            String cpId = bin.getCollectionPointId();
            if (cpId == null || !effectiveFilter.contains(cpId)) {
                continue;
            }

            BinReading latest = binReadingRepository.findTopByBinIdOrderByTsDesc(bin.getId());
            if (latest == null) {
                continue;
            }

            double fillPct = latest.getFillPct();
            double volumeL = (fillPct / 100.0) * BIN_CAPACITY_L;
            cpTotalVolume.merge(cpId, volumeL, Double::sum);
        }

        cpIdToVolumeLiters.clear();
        for (String cpId : effectiveFilter) {
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

    private VroomRequest buildVroomRequestForPointsWithVehicles(double[] depotCoords,
                                                                List<CollectionPoint> points,
                                                                Map<String, Double> cpIdToVolumeLiters,
                                                                Map<Integer, String> jobIdToCollectionPointId,
                                                                Map<Integer, String> vroomVehicleIdToVehicleId,
                                                                List<Vehicle> vehiclesPool) {

        if (vehiclesPool == null || vehiclesPool.isEmpty()) {
            throw new IllegalStateException("No available vehicles for tournee");
        }

        List<VroomVehicle> vroomVehicles = new ArrayList<>();
        int vroomVehicleId = 1;
        for (Vehicle v : vehiclesPool) {
            double capLiters = v.getCapacityVolumeL();
            if (capLiters <= 0) {
                continue;
            }
            VroomVehicle vv = new VroomVehicle();
            vv.setId(vroomVehicleId);
            vv.setStart(depotCoords);
            vv.setEnd(depotCoords);
            vv.setCapacity(new int[]{(int) Math.round(capLiters)});
            vroomVehicles.add(vv);
            vroomVehicleIdToVehicleId.put(vroomVehicleId, v.getId());
            vroomVehicleId++;
        }

        if (vroomVehicles.isEmpty()) {
            throw new IllegalStateException("No AVAILABLE vehicle with valid capacity in pool");
        }

        List<VroomJob> jobs = new ArrayList<>();
        int jobId = 1;
        for (CollectionPoint cp : points) {
            GeoJSONPoint loc = cp.getLocation();
            if (loc == null || loc.getCoordinates() == null) {
                continue;
            }

            double volumeLiters = cpIdToVolumeLiters.getOrDefault(cp.getId(), 0.0);
            if (volumeLiters <= 0) {
                throw new IllegalStateException("Inconsistent data: CP " + cp.getId()
                        + " is selected for collection but has volume <= 0");
            }

            VroomJob job = new VroomJob();
            job.setId(jobId);
            job.setAmount(new int[]{(int) Math.round(volumeLiters)});
            job.setService(300);
            job.setLocation(loc.getCoordinates());

            jobs.add(job);
            jobIdToCollectionPointId.put(jobId, cp.getId());
            jobId++;
        }

        if (jobs.isEmpty()) {
            throw new IllegalStateException("No jobs to send to VROOM");
        }

        VroomOptions options = new VroomOptions();
        options.setG(true);

        VroomRequest request = new VroomRequest();
        request.setVehicles(vroomVehicles);
        request.setJobs(jobs);
        request.setOptions(options);
        return request;
    }

    private Tournee buildTourneeFromVroom(TrashType type,
                                          VroomRoute route,
                                          Map<Integer, String> jobIdToCollectionPointId,
                                          String plannedVehicleId) {

        Tournee tournee = new Tournee();
        tournee.setTourneeType(type);
        tournee.setStatus(TourneeStatus.PLANNED);
        tournee.setPlannedKm(route.getDistance() / 1000.0);
        tournee.setPlannedCO2(0);
        tournee.setPlannedVehicleId(plannedVehicleId);
        tournee.setStartedAt(null);
        tournee.setFinishedAt(null);
        tournee.setGeometry(route.getGeometry());

        List<RouteStep> steps = new ArrayList<>();
        int order = 0;
        for (VroomStep step : route.getSteps()) {
            if (!"job".equalsIgnoreCase(step.getType())) {
                continue;
            }

            RouteStep routeStep = new RouteStep();
            routeStep.setOrder(order++);
            routeStep.setStatus(StepStatus.PENDING);

            Integer jobId = step.getJob();
            if (jobId != null) {
                routeStep.setCollectionPointId(jobIdToCollectionPointId.get(jobId));
            }
            steps.add(routeStep);
        }

        tournee.setSteps(steps);
        return tournee;
    }

    private Set<String> getCollectionPointIdsAlreadyCoveredForType(TrashType type) {
        Set<String> cpIds = new HashSet<>();
        List<Tournee> assignedTours = tourneeRepository.findByTourneeTypeAndStatus(type, TourneeStatus.PLANNED);
        for (Tournee t : assignedTours) {
            if (t.getSteps() == null) continue;
            for (RouteStep step : t.getSteps()) {
                if (step.getCollectionPointId() != null) {
                    cpIds.add(step.getCollectionPointId());
                }
            }
        }
        return cpIds;
    }

    private void markVehiclesBusy(Set<String> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return;
        }
        List<Vehicle> vehicles = vehicleRepository.findAllById(vehicleIds);
        if (vehicles.isEmpty()) {
            return;
        }
        for (Vehicle vehicle : vehicles) {
            vehicle.setBusy(true);
        }
        vehicleRepository.saveAll(vehicles);
    }
    @Override
    public List<TourneeResponseDTO> findByStatus(TourneeStatus status) {
        List<Tournee> tournees = tourneeRepository.findByStatus(status);
        return tournees.stream()
                .map(TourneeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void completeTournee(String tourneeId) {
        Tournee tournee = tourneeRepository.findById(tourneeId)
                .orElseThrow(() -> new IllegalArgumentException("Tournee not found: " + tourneeId));

        if (tournee.getStatus() == TourneeStatus.COMPLETED ||
                tournee.getStatus() == TourneeStatus.CANCELED) {
            return;
        }

        tournee.setStatus(TourneeStatus.COMPLETED);
        tournee.setFinishedAt(new Date()); // or Date.from(Instant.now())
        tourneeRepository.save(tournee);

        releaseResourcesForTournee(tournee);
    }

    private void releaseResourcesForTournee(Tournee tournee) {
        // 1) Vehicle
        String vehicleId = tournee.getPlannedVehicleId();
        if (vehicleId != null) {
            vehicleRepository.findById(vehicleId).ifPresent(v -> {
                v.setBusy(false);
                vehicleRepository.save(v);
            });
        }

        // 2) Employees
        List<TourneeAssignment> assignments =
                tourneeAssignmentRepository.findByTourneeId(tournee.getId());

        if (assignments == null || assignments.isEmpty()) {
            return;
        }

        Set<String> employeeIds = assignments.stream()
                .map(TourneeAssignment::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (employeeIds.isEmpty()) {
            return;
        }

        List<Employee> employees = (List<Employee>) employeeRepository.findAllById(employeeIds);

        for (Employee emp : employees) {
            // Does this employee still have assignments on PLANNED/IN_PROGRESS tours?
            List<TourneeAssignment> empAssignments =
                    tourneeAssignmentRepository.findByEmployeeId(emp.getId());

            boolean stillBusy = empAssignments.stream().anyMatch(a -> {
                if (a.getTourneeId().equals(tournee.getId())) {
                    return false;
                }
                return tourneeRepository.findById(a.getTourneeId())
                        .map(t -> t.getStatus() == TourneeStatus.PLANNED
                                || t.getStatus() == TourneeStatus.IN_PROGRESS)
                        .orElse(false);
            });

            if (!stillBusy) {
                emp.setStatus(EmployeeStatus.FREE);
            }
        }

        employeeRepository.saveAll(employees);
    }


}
