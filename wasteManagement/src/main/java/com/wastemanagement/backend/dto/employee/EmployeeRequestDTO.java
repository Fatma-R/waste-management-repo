package com.wastemanagement.backend.dto.employee;

import lombok.Data;

@Data
public class EmployeeRequestDTO {
    private String fullName;
    private String email;
    private String skill;
}
