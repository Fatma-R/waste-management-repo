package com.wastemanagement.backend.repository.user;

import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.model.user.EmployeeStatus;
import com.wastemanagement.backend.model.user.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends CrudRepository<Employee, String> {

    Optional<Employee> findByUser(User user);
    List<Employee> findByStatus(EmployeeStatus status);
    Employee findByUserEmail(String email);
}
