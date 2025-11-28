package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeAssignmentRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeAssignmentResponseDTO;
import com.wastemanagement.backend.mapper.tournee.TourneeAssignmentMapper;
import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.model.tournee.Tournee;
import com.wastemanagement.backend.model.tournee.TourneeAssignment;
import com.wastemanagement.backend.model.tournee.TourneeStatus;
import com.wastemanagement.backend.model.vehicle.Vehicle;
import com.wastemanagement.backend.model.vehicle.VehicleStatus;
import com.wastemanagement.backend.repository.VehicleRepository;
import com.wastemanagement.backend.repository.tournee.TourneeAssignmentRepository;
import com.wastemanagement.backend.repository.tournee.TourneeRepository;
import com.wastemanagement.backend.repository.user.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TourneeAssignmentServiceImpl implements TourneeAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TourneeAssignmentServiceImpl.class);

    private final TourneeAssignmentRepository repo;
    private final TourneeRepository tourneeRepository;
    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;

    public TourneeAssignmentServiceImpl(TourneeAssignmentRepository repo,
                                        TourneeRepository tourneeRepository,
                                        VehicleRepository vehicleRepository,
                                        EmployeeRepository employeeRepository) {

        this.repo = repo;
        this.tourneeRepository = tourneeRepository;
        this.vehicleRepository = vehicleRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<TourneeAssignmentResponseDTO> getAll() {
        return repo.findAll().stream()
                .map(TourneeAssignmentMapper::toResponseDTO)
                .toList();
    }

    @Override
    public Optional<TourneeAssignmentResponseDTO> getById(String id) {
        return repo.findById(id)
                .map(TourneeAssignmentMapper::toResponseDTO);
    }

    @Override
    public TourneeAssignmentResponseDTO create(TourneeAssignmentRequestDTO dto) {
        TourneeAssignment entity = TourneeAssignmentMapper.toEntity(dto);
        repo.save(entity);
        return TourneeAssignmentMapper.toResponseDTO(entity);
    }

    @Override
    public Optional<TourneeAssignmentResponseDTO> update(String id, TourneeAssignmentRequestDTO dto) {
        return repo.findById(id)
                .map(existing -> {
                    TourneeAssignmentMapper.merge(existing, dto);
                    repo.save(existing);
                    return TourneeAssignmentMapper.toResponseDTO(existing);
                });
    }

    @Override
    public boolean delete(String id) {
        return repo.findById(id)
                .map(a -> {
                    repo.delete(a);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public List<TourneeAssignmentResponseDTO> autoAssignForTournee(String tourneeId) {
        log.info("=== Auto-assign crew & vehicle for tournee {} ===", tourneeId);

        // 1) Charger la tournée
        Tournee tournee = tourneeRepository.findById(tourneeId)
                .orElseThrow(() -> new IllegalArgumentException("Tournee not found"));

        log.info("Loaded tournee id={}, status={}, plannedKm={}",
                tournee.getId(), tournee.getStatus(), tournee.getPlannedKm());

        if (tournee.getStatus() != TourneeStatus.PLANNED) {
            log.warn("Tournee {} has status {} (expected PLANNED) – aborting auto-assign",
                    tourneeId, tournee.getStatus());
            throw new IllegalStateException("Only PLANNED tournees can be assigned");
        }

        // 2) Déterminer un créneau (simple version : maintenant + durée estimée)
        Instant shiftStart = Instant.now();
        long estimatedMillis = estimateDurationMillis(tournee);
        Instant shiftEnd = shiftStart.plusMillis(estimatedMillis);

        log.info("Shift window for tournee {} -> start={}, end={} (≈ {} minutes)",
                tourneeId, shiftStart, shiftEnd, estimatedMillis / 60000);

        // 3) Choisir un véhicule
        Vehicle vehicle = pickVehicleForTournee(tournee);
        log.info("Selected vehicle id={} for tournee {}", vehicle.getId(), tourneeId);

        // 4) Choisir 3 employés
        List<Employee> crew = pickCrewForTournee();
        log.info("Selected crew for tournee {}: {} employees", tourneeId, crew.size());

        // 5) Construire les assignments
        List<TourneeAssignment> assignments = new ArrayList<>();
        for (Employee e : crew) {
            TourneeAssignment a = new TourneeAssignment();
            a.setTourneeId(tournee.getId());
            a.setVehicleId(vehicle.getId());
            a.setEmployeeId(e.getId());
            a.setShiftStart(shiftStart);
            a.setShiftEnd(shiftEnd);
            assignments.add(a);

            log.info("Creating assignment: tourneeId={}, vehicleId={}, employeeId={}, shiftStart={}, shiftEnd={}",
                    tournee.getId(), vehicle.getId(), e.getId(), shiftStart, shiftEnd);
        }

        // 6) Sauvegarder et mapper
        List<TourneeAssignment> saved = repo.saveAll(assignments);
        log.info("Saved {} assignments for tournee {}", saved.size(), tourneeId);

        return saved.stream()
                .map(TourneeAssignmentMapper::toResponseDTO)
                .toList();
    }

    // ---------------------------------------------------------
    // Helpers internes
    // ---------------------------------------------------------
    private long estimateDurationMillis(Tournee tournee) {
        double plannedKm = tournee.getPlannedKm(); // primitive double

        if (plannedKm <= 0) {
            log.warn("Tournee {} has invalid plannedKm={} – falling back to 10 km",
                    tournee.getId(), plannedKm);
            plannedKm = 10.0;
        }

        double avgSpeedKmh = 25.0; // vitesse moyenne
        double hours = plannedKm / avgSpeedKmh;
        long millis = (long) (hours * 3600 * 1000);

        log.info("Estimated duration for tournee {}: plannedKm={}, avgSpeedKmh={}, millis={}",
                tournee.getId(), plannedKm, avgSpeedKmh, millis);

        return millis;
    }

    private Vehicle pickVehicleForTournee(Tournee tournee) {
        log.info("Picking vehicle for tournee {}", tournee.getId());
        return vehicleRepository.findFirstByStatus(VehicleStatus.AVAILABLE)
                .map(v -> {
                    log.info("Found AVAILABLE vehicle: id={}, plate={}", v.getId(), v.getPlateNumber());
                    return v;
                })
                .orElseThrow(() -> {
                    log.error("No AVAILABLE vehicle for tournee {}", tournee.getId());
                    return new IllegalStateException("No AVAILABLE vehicle");
                });
    }

    private List<Employee> pickCrewForTournee() {
        log.info("Picking crew (3 employees) for tournee");

        // Version très simplifiée : on prend tous les employés
        List<Employee> active = (List<Employee>) employeeRepository.findAll();

        log.info("Total employees found in DB = {}", active.size());
        for (Employee e : active) {
            // toString() de Employee (Lombok @Data ou autre) donnera déjà des infos utiles
            log.info("Candidate employee: {}", e);
        }

        if (active.size() < 3) {
            log.error("Not enough employees to assign tournee. Required=3, found={}", active.size());
            throw new IllegalStateException("Not enough active employees to assign this tournee");
        }

        List<Employee> crew = active.subList(0, 3);
        log.info("Selected crew (first 3 employees):");
        for (Employee e : crew) {
            log.info("  -> crew member: {}", e);
        }

        return crew;
    }
}
