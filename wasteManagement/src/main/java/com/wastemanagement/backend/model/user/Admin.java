package com.wastemanagement.backend.model.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;


@Data
@NoArgsConstructor
@Document(collection = "admins")
public class Admin extends User {
    public Admin(String fullName, String email) {
        super(fullName, email, null);
    }
}