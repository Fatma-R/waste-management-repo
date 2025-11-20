package com.wastemanagement.backend.dto.user;

import lombok.Data;
import java.util.Set;

@Data
public class UserDTO {
    private String fullName;
    private String email;
    private String password;
    private Set<String> roles;
}