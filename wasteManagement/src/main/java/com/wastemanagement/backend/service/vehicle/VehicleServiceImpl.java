package com.wastemanagement.backend.service.vehicle;

import com.wastemanagement.backend.dto.tournee.TourneeAssignmentResponseDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleLocationUpdateDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleRequestDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleResponseDTO;
import com.wastemanagement.backend.mapper.VehicleMapper;
import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.tournee.StepStatus;
import com.wastemanagement.backend.model.tournee.Tournee;
import com.wastemanagement.backend.model.tournee.TourneeStatus;
import com.wastemanagement.backend.model.vehicle.FuelType;
import com.wastemanagement.backend.model.vehicle.Vehicle;
import com.wastemanagement.backend.model.vehicle.VehicleStatus;
import com.wastemanagement.backend.repository.VehicleRepository;
import com.wastemanagement.backend.repository.tournee.TourneeRepository;
import com.wastemanagement.backend.service.tournee.TourneeAssignmentService;
import com.wastemanagement.backend.service.tournee.DepotService;
import com.wastemanagement.backend.service.tournee.TourneeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class VehicleServiceImpl implements VehicleService {

    @Autowired
    private VehicleRepository repository;
    @Autowired
    private TourneeAssignmentService assignmentService;
    @Autowired
    private VehicleMapper mapper;
    @Autowired
    private DepotService depotService;
    @Autowired
    private TourneeService tourneeService;
    @Autowired
    private TourneeRepository tourneeRepository;



    private final Map<String, List<SseEmitter>> locationEmitters = new ConcurrentHashMap<>();
    private static final double LOCATION_TOLERANCE = 1e-5;

    @Override
    public VehicleResponseDTO createVehicle(VehicleRequestDTO dto) {
        Vehicle vehicle = mapper.toEntity(dto);
        Vehicle saved = repository.save(vehicle);
        return mapper.toResponseDTO(saved);
    }

    @Override
    public List<VehicleResponseDTO> getAllVehicles(int page, int size) {
        return repository.findAll(PageRequest.of(page, size))
                .stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public VehicleResponseDTO getVehicleById(String id) {
        Optional<Vehicle> vehicle = repository.findById(id);
        return vehicle.map(mapper::toResponseDTO).orElse(null);
    }

    @Override
    public VehicleResponseDTO updateVehicle(String id, VehicleRequestDTO dto) {
        Optional<Vehicle> optionalVehicle = repository.findById(id);
        if (optionalVehicle.isEmpty()) {
            return null;
        }

        Vehicle vehicle = optionalVehicle.get();
        mapper.updateEntity(dto, vehicle);
        return mapper.toResponseDTO(repository.save(vehicle));
    }

    @Override
    public boolean deleteVehicle(String id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    @Override
    public List<VehicleResponseDTO> getVehiclesByStatus(String status) {
        return repository.findByStatus(Enum.valueOf(VehicleStatus.class, status))
                .stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VehicleResponseDTO> getVehiclesByFuelType(FuelType fuelType) {
        return repository.findByFuelType(fuelType)
                .stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VehicleResponseDTO> getAvailableVehiclesForTournee(TourneeResponseDTO plannedTournee) {
        if (plannedTournee.getStartedAt() == null || plannedTournee.getFinishedAt() == null) {
            throw new IllegalArgumentException("La tournee doit avoir une date de debut et de fin planifiee");
        }

        Instant tourneeStart = plannedTournee.getStartedAt().toInstant();
        Instant tourneeEnd = plannedTournee.getFinishedAt().toInstant();

        List<TourneeAssignmentResponseDTO> allAssignments = assignmentService.getAll();

        List<String> busyVehicleIds = allAssignments.stream()
                .filter(a -> timesOverlap(a.getShiftStart(), a.getShiftEnd(), tourneeStart, tourneeEnd))
                .map(TourneeAssignmentResponseDTO::getVehicleId)
                .collect(Collectors.toList());

        return repository.findByStatusAndBusyFalse(VehicleStatus.AVAILABLE).stream()
                .filter(v -> !busyVehicleIds.contains(v.getId()))
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public GeoJSONPoint getCurrentLocation(String vehicleId) {
        return repository.findById(vehicleId)
                .map(Vehicle::getCurrentLocation)
                .orElse(null);
    }

    @Override
    public VehicleResponseDTO updateCurrentLocation(String vehicleId, VehicleLocationUpdateDTO locationUpdate) {
        if (locationUpdate == null || locationUpdate.getLatitude() == null || locationUpdate.getLongitude() == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }

        Optional<Vehicle> optionalVehicle = repository.findById(vehicleId);
        if (optionalVehicle.isEmpty()) {
            return null;
        }

        Vehicle vehicle = optionalVehicle.get();
        GeoJSONPoint newLocation = new GeoJSONPoint(locationUpdate.getLongitude(), locationUpdate.getLatitude());
        vehicle.setCurrentLocation(newLocation);

        Vehicle saved = repository.save(vehicle);

        notifyLocationListeners(vehicle.getId(), saved.getCurrentLocation());

        // 🔹 NEW: use GPS ONLY to *suggest* completion
        if (isAtMainDepot(newLocation)) {
            maybeCompleteToursForVehicle(saved);
        }

        return mapper.toResponseDTO(saved);
    }

    private void maybeCompleteToursForVehicle(Vehicle vehicle) {
        // Find IN_PROGRESS tours that are using this vehicle
        List<Tournee> inProgressTours = tourneeRepository.findByStatus(TourneeStatus.IN_PROGRESS);

        inProgressTours.stream()
                .filter(t -> vehicle.getId().equals(t.getPlannedVehicleId()))
                .forEach(t -> {
                    boolean allStepsDone = t.getSteps() != null
                            && t.getSteps().stream()
                            .noneMatch(step -> step.getStatus() == StepStatus.PENDING);

                    if (allStepsDone) {
                        // This will set status=COMPLETED and free vehicle + employees
                        tourneeService.completeTournee(t.getId());
                    }
                });
    }

    @Override
    public SseEmitter streamLocation(String vehicleId) {
        SseEmitter emitter = new SseEmitter(0L);

        locationEmitters
                .computeIfAbsent(vehicleId, id -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(vehicleId, emitter));
        emitter.onTimeout(() -> removeEmitter(vehicleId, emitter));
        emitter.onError((ex) -> removeEmitter(vehicleId, emitter));

        GeoJSONPoint latest = getCurrentLocation(vehicleId);
        if (latest != null) {
            try {
                emitter.send(SseEmitter.event().name("location").data(latest));
            } catch (Exception ignored) {
                removeEmitter(vehicleId, emitter);
            }
        }

        return emitter;
    }

    private void notifyLocationListeners(String vehicleId, GeoJSONPoint location) {
        List<SseEmitter> emitters = locationEmitters.get(vehicleId);
        if (emitters == null || emitters.isEmpty() || location == null) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("location").data(location));
            } catch (Exception ex) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
        }
    }

    private void removeEmitter(String vehicleId, SseEmitter emitter) {
        List<SseEmitter> emitters = locationEmitters.get(vehicleId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }

    private boolean isAtMainDepot(GeoJSONPoint location) {
        if (location == null || location.getCoordinates() == null || location.getCoordinates().length < 2) {
            return false;
        }
        try {
            com.wastemanagement.backend.model.tournee.Depot depot = depotService.getMainDepotEntityOrThrow();
            GeoJSONPoint depotLocation = depot.getLocation();
            if (depotLocation == null || depotLocation.getCoordinates() == null || depotLocation.getCoordinates().length < 2) {
                return false;
            }
            double[] depotCoords = depotLocation.getCoordinates();
            double[] vehicleCoords = location.getCoordinates();
            return Math.abs(depotCoords[0] - vehicleCoords[0]) < LOCATION_TOLERANCE
                    && Math.abs(depotCoords[1] - vehicleCoords[1]) < LOCATION_TOLERANCE;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Checks if two time ranges overlap.
     */
    private boolean timesOverlap(Instant start1, Instant end1, Instant start2, Instant end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
