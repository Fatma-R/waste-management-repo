package com.wastemanagement.backend.service.user;

import com.wastemanagement.backend.dto.user.EmployeeRequestDTO;
import com.wastemanagement.backend.mapper.employee.EmployeeMapper;
import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.model.user.Skill;
import com.wastemanagement.backend.repository.user.EmployeeRepository;
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
                dto.getSkill()
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
        EmployeeMapper.updateEntity(emp, dto);
        return employeeRepo.save(emp);
    }

    @Override
    public void deleteEmployee(String id) {
        employeeRepo.deleteById(id);
    }
}
