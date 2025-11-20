package com.wastemanagement.backend.dto.user;

import lombok.Data;

@Data
public class EmployeeResponseDTO {
    private String id;
    private String fullName;
    private String email;
    private String skill;
}
