package com.wastemanagement.backend.service.user;

import com.wastemanagement.backend.dto.tournee.TourneeAssignmentResponseDTO;
import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.dto.user.EmployeeRequestDTO;
import com.wastemanagement.backend.dto.user.EmployeeResponseDTO;
import com.wastemanagement.backend.dto.vehicle.VehicleResponseDTO;
import com.wastemanagement.backend.mapper.employee.EmployeeMapper;
import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.model.user.Skill;
import com.wastemanagement.backend.model.user.User;
import com.wastemanagement.backend.repository.UserRepository;
import com.wastemanagement.backend.repository.user.EmployeeRepository;
import com.wastemanagement.backend.service.tournee.TourneeAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepo;
    private final UserRepository userRepo;
    @Autowired
    private TourneeAssignmentService assignmentService;

    @Override
    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO dto) {
        Employee emp = EmployeeMapper.toEntity(dto);
        Employee saved = employeeRepo.save(emp);
        return EmployeeMapper.toResponse(saved);
    }

    @Override
    public EmployeeResponseDTO createFromUser(User user, Skill skill) {
        Employee emp = new Employee();
        emp.setUser(user);
        emp.setSkill(skill);
        Employee saved = employeeRepo.save(emp);
        return EmployeeMapper.toResponse(saved);
    }

    @Override
    public EmployeeResponseDTO getEmployeeById(String id) {
        Employee emp = employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return EmployeeMapper.toResponse(emp);
    }

    @Override
    public List<EmployeeResponseDTO> getAllEmployees() {
        return ((List<Employee>) employeeRepo.findAll())
                .stream()
                .map(EmployeeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public EmployeeResponseDTO updateEmployee(String id, EmployeeRequestDTO dto) {
        Employee emp = employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        EmployeeMapper.updateEntity(emp, dto);
        Employee saved = employeeRepo.save(emp);
        return EmployeeMapper.toResponse(saved);
    }

    @Override
    public void deleteEmployee(String id) {
        employeeRepo.deleteById(id);
    }

    @Override
    public void deleteEmployeeAndUserByEmployeeId(String employeeId) {
        Employee emp = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        User user = emp.getUser();

        employeeRepo.delete(emp);

        if (user != null && user.getId() != null) {
            userRepo.deleteById(user.getId());
        }
    }

    // ======================== Nouvelle méthode ========================
    @Override
    public List<EmployeeResponseDTO> getAvailableEmployeeForTournee(TourneeResponseDTO plannedTournee) {
        if (plannedTournee.getStartedAt() == null || plannedTournee.getFinishedAt() == null) {
            throw new IllegalArgumentException("La tournée doit avoir une date de début et de fin planifiée");
        }

        Instant tourneeStart = plannedTournee.getStartedAt().toInstant();
        Instant tourneeEnd = plannedTournee.getFinishedAt().toInstant();

        // Récupérer toutes les assignations
        List<TourneeAssignmentResponseDTO> allAssignments = assignmentService.getAll();

        // IDs des employé déjà pris sur cette plage
        List<String> busyEmployeesIds = allAssignments.stream()
                .filter(a -> timesOverlap(a.getShiftStart(), a.getShiftEnd(), tourneeStart, tourneeEnd))
                .map(TourneeAssignmentResponseDTO::getEmployeeId)
                .collect(Collectors.toList());

        // Retourner les employés libres
        return ((List<Employee>) employeeRepo.findAll()).stream()
                .filter(e -> !busyEmployeesIds.contains(e.getId()))
                .map(EmployeeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public EmployeeResponseDTO getEmployeeByEmail(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        Employee emp = employeeRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Employee not found for user: " + email));

        return EmployeeMapper.toResponse(emp);
    }

    /**
     * Vérifie si deux plages horaires se chevauchent
     */
    private boolean timesOverlap(Instant start1, Instant end1, Instant start2, Instant end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
