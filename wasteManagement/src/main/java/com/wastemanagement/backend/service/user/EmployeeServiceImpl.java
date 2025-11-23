package com.wastemanagement.backend.service.user;

import com.wastemanagement.backend.dto.user.EmployeeRequestDTO;
import com.wastemanagement.backend.dto.user.EmployeeResponseDTO;
import com.wastemanagement.backend.mapper.employee.EmployeeMapper;
import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.model.user.Skill;
import com.wastemanagement.backend.model.user.User;
import com.wastemanagement.backend.repository.UserRepository;
import com.wastemanagement.backend.repository.user.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepo;
    private final UserRepository userRepo;

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
}
