package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeAssignmentRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeAssignmentResponseDTO;
import com.wastemanagement.backend.mapper.tournee.TourneeAssignmentMapper;
import com.wastemanagement.backend.model.tournee.Tournee;
import com.wastemanagement.backend.model.tournee.TourneeAssignment;
import com.wastemanagement.backend.model.tournee.TourneeStatus;
import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.model.vehicle.Vehicle;
import com.wastemanagement.backend.model.vehicle.VehicleStatus;
import com.wastemanagement.backend.repository.VehicleRepository;
import com.wastemanagement.backend.repository.tournee.TourneeAssignmentRepository;
import com.wastemanagement.backend.repository.tournee.TourneeRepository;
import com.wastemanagement.backend.repository.user.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TourneeAssignmentServiceImpl implements TourneeAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TourneeAssignmentServiceImpl.class);
    private static final double AVG_SPEED_KMH = 25.0;

    private final TourneeAssignmentRepository repo;
    private final TourneeRepository tourneeRepository;
    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public List<TourneeAssignmentResponseDTO> getAll() {
        return repo.findAll().stream()
                .map(TourneeAssignmentMapper::toResponseDTO)
                .toList();
    }

    @Override
    public Optional<TourneeAssignmentResponseDTO> getById(String id) {
        return repo.findById(id).map(TourneeAssignmentMapper::toResponseDTO);
    }

    @Override
    public TourneeAssignmentResponseDTO create(TourneeAssignmentRequestDTO dto) {
        TourneeAssignment entity = TourneeAssignmentMapper.toEntity(dto);
        return TourneeAssignmentMapper.toResponseDTO(repo.save(entity));
    }

    @Override
    public Optional<TourneeAssignmentResponseDTO> update(String id, TourneeAssignmentRequestDTO dto) {
        Optional<TourneeAssignment> existing = repo.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        TourneeAssignment entity = existing.get();
        TourneeAssignmentMapper.merge(entity, dto);
        return Optional.of(TourneeAssignmentMapper.toResponseDTO(repo.save(entity)));
    }

    @Override
    public boolean delete(String id) {
        if (!repo.existsById(id)) {
            return false;
        }
        repo.deleteById(id);
        return true;
    }

    @Override
    public List<TourneeAssignmentResponseDTO> autoAssignForTournee(String tourneeId) {
        log.info("=== Auto-assign crew & vehicle for tournee {} ===", tourneeId);

        Tournee tournee = tourneeRepository.findById(tourneeId)
                .orElseThrow(() -> new IllegalArgumentException("Tournee not found"));

        if (tournee.getStatus() != TourneeStatus.PLANNED) {
            throw new IllegalStateException("Only PLANNED tournees can be assigned");
        }

        Instant shiftStart = Instant.now();
        long estimatedMillis = estimateDurationMillis(tournee);
        Instant shiftEnd = shiftStart.plusMillis(estimatedMillis);

        Vehicle vehicle = pickVehicleForTournee(tournee);
        markVehicleBusy(vehicle);

        List<Employee> crew = pickCrewForTournee();

        List<TourneeAssignment> assignments = new ArrayList<>();
        for (Employee e : crew) {
            TourneeAssignment a = new TourneeAssignment();
            a.setTourneeId(tournee.getId());
            a.setVehicleId(vehicle.getId());
            a.setEmployeeId(e.getId());
            a.setShiftStart(shiftStart);
            a.setShiftEnd(shiftEnd);
            assignments.add(a);
        }

        List<TourneeAssignment> saved = repo.saveAll(assignments);
        tournee.setStatus(TourneeStatus.ASSIGNED);
        tourneeRepository.save(tournee);

        return saved.stream()
                .map(TourneeAssignmentMapper::toResponseDTO)
                .toList();
    }

    // Helpers
    private long estimateDurationMillis(Tournee tournee) {
        double plannedKm = tournee.getPlannedKm();
        if (plannedKm <= 0) {
            plannedKm = 10.0;
        }
        double hours = plannedKm / AVG_SPEED_KMH;
        return (long) (hours * 3600 * 1000);
    }

    private Vehicle pickVehicleForTournee(Tournee tournee) {
        return vehicleRepository.findFirstByStatusAndBusyFalse(VehicleStatus.AVAILABLE)
                .orElseThrow(() -> new IllegalStateException("No AVAILABLE vehicle for tournee " + tournee.getId()));
    }

    private List<Employee> pickCrewForTournee() {
        List<Employee> active = (List<Employee>) employeeRepository.findAll();
        if (active.size() < 3) {
            throw new IllegalStateException("Not enough active employees to assign this tournee");
        }
        return active.subList(0, 3);
    }

    private void markVehicleBusy(Vehicle vehicle) {
        if (vehicle == null || vehicle.isBusy()) {
            return;
        }
        vehicle.setBusy(true);
        vehicleRepository.save(vehicle);
    }
}
