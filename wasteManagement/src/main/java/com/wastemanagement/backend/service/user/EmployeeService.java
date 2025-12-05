package com.wastemanagement.backend.service.user;

import com.wastemanagement.backend.dto.tournee.TourneeResponseDTO;
import com.wastemanagement.backend.dto.user.EmployeeRequestDTO;
import com.wastemanagement.backend.dto.user.EmployeeResponseDTO;
import com.wastemanagement.backend.model.user.Skill;
import com.wastemanagement.backend.model.user.User;

import java.util.List;

public interface EmployeeService {

    // Optionnel: toujours possible, même si l\'endpoint REST de création est dans AuthController
    EmployeeResponseDTO createEmployee(EmployeeRequestDTO dto);

    // Utilisé par AuthController lors du signup
    EmployeeResponseDTO createFromUser(User user, Skill skill);

    EmployeeResponseDTO getEmployeeById(String id);

    List<EmployeeResponseDTO> getAllEmployees();

    EmployeeResponseDTO updateEmployee(String id, EmployeeRequestDTO dto);

    void deleteEmployee(String id);

    void deleteEmployeeAndUserByEmployeeId(String employeeId);

    List<EmployeeResponseDTO> getAvailableEmployeeForTournee(TourneeResponseDTO plannedTournee);

    EmployeeResponseDTO getEmployeeByEmail(String email);
}
