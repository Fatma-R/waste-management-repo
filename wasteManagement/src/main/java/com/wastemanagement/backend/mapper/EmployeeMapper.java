package com.wastemanagement.backend.mapper;

import com.wastemanagement.backend.dto.employee.EmployeeRequestDTO;
import com.wastemanagement.backend.dto.employee.EmployeeResponseDTO;
import com.wastemanagement.backend.model.employee.Employee;
import com.wastemanagement.backend.model.employee.Skill;

public class EmployeeMapper {

    public static Employee toEntity(EmployeeRequestDTO dto) {
        Employee emp = new Employee();
        emp.setFullName(dto.getFullName());
        emp.setEmail(dto.getEmail());
        emp.setSkill(dto.getSkill() != null ? Skill.valueOf(dto.getSkill()) : null);
        return emp;
    }

    public static EmployeeResponseDTO toResponse(Employee emp) {
        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setId(emp.getId());
        dto.setFullName(emp.getFullName());
        dto.setEmail(emp.getEmail());
        dto.setSkill(emp.getSkill() != null ? emp.getSkill().name() : null);
        return dto;
    }

    public static void updateEntity(Employee emp, EmployeeRequestDTO dto) {
        if (dto.getFullName() != null) {
            emp.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null) {
            emp.setEmail(dto.getEmail());
        }
        if (dto.getSkill() != null) {
            emp.setSkill(Skill.valueOf(dto.getSkill()));
        }
    }

}
