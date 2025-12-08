package com.wastemanagement.backend.service.tournee;

import com.wastemanagement.backend.dto.tournee.TourneeAssignmentRequestDTO;
import com.wastemanagement.backend.dto.tournee.TourneeAssignmentResponseDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.mapper.tournee.TourneeAssignmentMapper;
import com.wastemanagement.backend.mapper.tournee.TourneeMapper;
import com.wastemanagement.backend.model.tournee.Tournee;
import com.wastemanagement.backend.model.tournee.TourneeAssignment;
import com.wastemanagement.backend.model.tournee.TourneeStatus;
import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.model.user.EmployeeStatus;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourneeAssignmentServiceImpl implements TourneeAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TourneeAssignmentServiceImpl.class);
    private static final double AVG_SPEED_KMH = 25.0;

    private final TourneeAssignmentRepository repo;
    private final TourneeRepository tourneeRepository;
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
        if (existing.isEmpty()) return Optional.empty();

        TourneeAssignment entity = existing.get();
        TourneeAssignmentMapper.merge(entity, dto);
        return Optional.of(TourneeAssignmentMapper.toResponseDTO(repo.save(entity)));
    }

    @Override
    public boolean delete(String id) {
        if (!repo.existsById(id)) return false;
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

        // compute start/end of shift based on distance
        Instant shiftStart = Instant.now();
        long estimatedMillis = estimateDurationMillis(tournee);
        Instant shiftEnd = shiftStart.plusMillis(estimatedMillis);

        // pick only employees available in this window + mark them BUSY
        // while it is true that right now any assignment really only needs the flag to check availability,
        // I'm checking the time window mainly for future proofing, and also for data consistency.
        List<Employee> crew = pickCrewForTournee(shiftStart, shiftEnd);

        List<TourneeAssignment> assignments = new ArrayList<>();
        for (Employee e : crew) {
            TourneeAssignment a = new TourneeAssignment();
            a.setTourneeId(tournee.getId());
            a.setVehicleId(tournee.getPlannedVehicleId());
            a.setEmployeeId(e.getId());
            a.setShiftStart(shiftStart);
            a.setShiftEnd(shiftEnd);
            assignments.add(a);
        }

        List<TourneeAssignment> saved = repo.saveAll(assignments);

        log.info("Saved {} assignments (crew + tournee vehicle) for tournee {}", saved.size(), tourneeId);

        // once assigned → IN_PROGRESS (already your rule)
        tournee.setStatus(TourneeStatus.IN_PROGRESS);
        tourneeRepository.save(tournee);

        return saved.stream()
                .map(TourneeAssignmentMapper::toResponseDTO)
                .toList();
    }


    @Override
    public List<TourneeResponseDTO> getInProgressTourneesForEmployee(String employeeId) {
        List<String> tourneeIds = repo.findByEmployeeId(employeeId).stream()
                .map(TourneeAssignment::getTourneeId)
                .toList();

        if (tourneeIds.isEmpty()) return List.of();

        return tourneeRepository.findByStatusAndIdIn(TourneeStatus.IN_PROGRESS, tourneeIds)
                .stream()
                .map(TourneeMapper::toResponse)
                .toList();
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private long estimateDurationMillis(Tournee tournee) {
        double plannedKm = tournee.getPlannedKm();
        if (plannedKm <= 0) plannedKm = 10.0;
        double hours = plannedKm / AVG_SPEED_KMH;
        return (long) (hours * 3600 * 1000);
    }

    private List<Employee> pickCrewForTournee(Instant shiftStart, Instant shiftEnd) {
        // All existing assignments
        List<TourneeAssignment> allAssignments = repo.findAll();

        // Employees busy in the requested time window (overlapping shifts)
        Set<String> busyEmployeeIds = allAssignments.stream()
                .filter(a -> timesOverlap(a.getShiftStart(), a.getShiftEnd(), shiftStart, shiftEnd))
                .map(TourneeAssignment::getEmployeeId)
                .collect(Collectors.toSet());

        // Candidates = employees not busy in this window AND not logically BUSY
        List<Employee> candidates = ((List<Employee>) employeeRepository.findAll())
                .stream()
                .filter(e -> {
                    EmployeeStatus status = e.getStatus();
                    boolean logicallyFree = (status == null || status == EmployeeStatus.FREE);
                    return logicallyFree && !busyEmployeeIds.contains(e.getId());
                })
                .collect(Collectors.toList());

        if (candidates.size() < 3) {
            throw new IllegalStateException("Not enough available employees to assign this tournee");
        }

        // For now, simply pick the first 3
        List<Employee> chosen = candidates.subList(0, 3);

        // 🔹 Mark them BUSY
        chosen.forEach(e -> e.setStatus(EmployeeStatus.BUSY));
        employeeRepository.saveAll(chosen);

        return chosen;
    }

    private boolean timesOverlap(Instant start1, Instant end1, Instant start2, Instant end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    @Override
    public List<TourneeAssignmentResponseDTO> getAssignmentsForTournee(String tourneeId) {
        return repo.findByTourneeId(tourneeId).stream()
                .map(TourneeAssignmentMapper::toResponseDTO)
                .toList();
    }

}
