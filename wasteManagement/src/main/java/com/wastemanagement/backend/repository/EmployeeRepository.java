package com.wastemanagement.backend.repository;

import com.wastemanagement.backend.model.employee.Employee;
import org.springframework.data.repository.CrudRepository;

public interface EmployeeRepository extends CrudRepository<Employee, String> {

}
