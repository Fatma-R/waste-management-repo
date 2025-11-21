package com.wastemanagement.backend.dto.user;

import com.wastemanagement.backend.model.user.Skill;
import lombok.Data;

@Data
public class EmployeeRequestDTO {
    private String fullName;
    private String email;
    private Skill skill;
}
