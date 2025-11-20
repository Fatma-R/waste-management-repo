package com.wastemanagement.backend.model.employee;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;


@Data

@Document(collection = "admins")
public class Admin extends User {


}