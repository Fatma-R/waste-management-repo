package com.wastemanagement.backend.controller.user;

import com.wastemanagement.backend.dto.user.EmployeeRequestDTO;
import com.wastemanagement.backend.dto.user.EmployeeResponseDTO;
import com.wastemanagement.backend.mapper.employee.EmployeeMapper;
import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.service.user.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public EmployeeResponseDTO create(@RequestBody EmployeeRequestDTO dto) {
        Employee emp = employeeService.createEmployee(dto);
        return EmployeeMapper.toResponse(emp);
    }

    @GetMapping("/{id}")
    public EmployeeResponseDTO getById(@PathVariable String id) {
        Employee emp = employeeService.getEmployeeById(id);
        return EmployeeMapper.toResponse(emp);
    }

    @GetMapping
    public List<EmployeeResponseDTO> getAll() {
        return employeeService.getAllEmployees()
                .stream()
                .map(EmployeeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public EmployeeResponseDTO update(
            @PathVariable String id,
            @RequestBody EmployeeRequestDTO dto) {
        Employee emp = employeeService.updateEmployee(id, dto);
        return EmployeeMapper.toResponse(emp);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        employeeService.deleteEmployee(id);
    }
}
