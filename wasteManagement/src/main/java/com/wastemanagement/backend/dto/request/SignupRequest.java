package com.wastemanagement.backend.dto.request;

import com.wastemanagement.backend.model.user.Skill;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    private String fullName;
    private String email;
    private String password;
    private Set<String> roles;

    private Skill skill;
}