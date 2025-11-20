package com.wastemanagement.backend.model.employee;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "employees")
public class Employee extends User {
    private Skill skill;

    public Employee(String fullName, String email, String password, Skill skill) {
        super(fullName, email, password);
        this.skill = skill;
    }

    public Employee(String fullName, String email, Skill skill) {
        super(fullName, email, null);
        this.skill = skill;
    }
}