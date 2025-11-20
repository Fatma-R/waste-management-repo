package com.wastemanagement.backend.repository.user;

import com.wastemanagement.backend.model.user.Employee;
import org.springframework.data.repository.CrudRepository;

public interface EmployeeRepository extends CrudRepository<Employee, String> {

}
