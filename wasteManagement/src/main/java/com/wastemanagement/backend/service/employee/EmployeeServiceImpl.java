package com.wastemanagement.backend.service.employee;

import com.wastemanagement.backend.dto.employee.EmployeeDTO;
import com.wastemanagement.backend.dto.employee.EmployeeRequestDTO;
import com.wastemanagement.backend.model.employee.Employee;
import com.wastemanagement.backend.model.employee.Skill;
import com.wastemanagement.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepo;

    @Override
    public Employee createEmployee(EmployeeRequestDTO dto) {
        Employee emp = new Employee(
                dto.getFullName(),
                dto.getEmail(),
                Skill.valueOf(dto.getSkill()) // if you use enum
        );
        return employeeRepo.save(emp);
    }

    @Override
    public Employee getEmployeeById(String id) {
        return employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    @Override
    public List<Employee> getAllEmployees() {
        return (List<Employee>) employeeRepo.findAll();
    }

    @Override
    public Employee updateEmployee(String id, EmployeeRequestDTO dto) {
        Employee emp = getEmployeeById(id);

        emp.setFullName(dto.getFullName());
        emp.setEmail(dto.getEmail());
        emp.setSkill(Skill.valueOf(dto.getSkill()));

        return employeeRepo.save(emp);
    }

    @Override
    public void deleteEmployee(String id) {
        employeeRepo.deleteById(id);
    }
}
