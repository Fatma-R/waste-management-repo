package com.wastemanagement.backend.service.user;

import com.wastemanagement.backend.dto.user.EmployeeRequestDTO;
import com.wastemanagement.backend.model.user.Employee;

import java.util.List;

public interface EmployeeService {

    Employee createEmployee(EmployeeRequestDTO dto);

    Employee getEmployeeById(String id);

    List<Employee> getAllEmployees();

    Employee updateEmployee(String id, EmployeeRequestDTO dto);

    void deleteEmployee(String id);
}
