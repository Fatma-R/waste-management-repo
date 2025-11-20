package com.wastemanagement.backend.dto.employee;

import lombok.Data;

@Data
public class EmployeeResponseDTO {
    private String id;
    private String fullName;
    private String email;
    private String skill;
}
