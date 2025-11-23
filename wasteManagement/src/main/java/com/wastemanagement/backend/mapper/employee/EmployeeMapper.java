package com.wastemanagement.backend.mapper.employee;

import com.wastemanagement.backend.dto.user.EmployeeRequestDTO;
import com.wastemanagement.backend.dto.user.EmployeeResponseDTO;
import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.model.user.User;

public class EmployeeMapper {

    public static Employee toEntity(EmployeeRequestDTO dto) {
        if (dto == null) return null;

        Employee emp = new Employee();

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        emp.setUser(user);

        emp.setSkill(dto.getSkill());
        return emp;
    }

    public static EmployeeResponseDTO toResponse(Employee emp) {
        if (emp == null) return null;

        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setId(emp.getId());

        if (emp.getUser() != null) {
            dto.setFullName(emp.getUser().getFullName());
            dto.setEmail(emp.getUser().getEmail());
        }

        dto.setSkill(emp.getSkill());
        return dto;
    }

    public static void updateEntity(Employee emp, EmployeeRequestDTO dto) {
        if (emp == null || dto == null) return;

        if (emp.getUser() == null) {
            emp.setUser(new User());
        }

        if (dto.getFullName() != null) {
            emp.getUser().setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null) {
            emp.getUser().setEmail(dto.getEmail());
        }
        if (dto.getSkill() != null) {
            emp.setSkill(dto.getSkill());
        }
    }
}
