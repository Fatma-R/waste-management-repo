package com.wastemanagement.backend.mapper.employee;

import com.wastemanagement.backend.dto.user.EmployeeRequestDTO;
import com.wastemanagement.backend.dto.user.EmployeeResponseDTO;
import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.model.user.Skill;

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
