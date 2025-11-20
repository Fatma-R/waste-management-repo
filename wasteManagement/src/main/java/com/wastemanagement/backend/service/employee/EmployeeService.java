package com.wastemanagement.backend.service.employee;

import com.wastemanagement.backend.dto.employee.EmployeeDTO;
import com.wastemanagement.backend.dto.employee.EmployeeRequestDTO;
import com.wastemanagement.backend.model.employee.Employee;

import java.util.List;

public interface EmployeeService {

    Employee createEmployee(EmployeeRequestDTO dto);

    Employee getEmployeeById(String id);

    List<Employee> getAllEmployees();

    Employee updateEmployee(String id, EmployeeRequestDTO dto);

    void deleteEmployee(String id);
}
