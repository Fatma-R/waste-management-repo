// java
package com.wastemanagement.backend.model.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "admins")
public class Admin {

    @Id
    private String id;

    @DBRef
    private User user;

    public Admin(User user) {
        this.user = user;
    }
}
