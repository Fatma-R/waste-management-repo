// java
package com.wastemanagement.backend.model.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "employees")
public class Employee {

    @Id
    private String id;

    @DBRef
    private User user;

    private Skill skill;
}
