package com.wastemanagement.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("helloItems") // name of the collection
public class HelloItem {

    @Id
    private String id;

    private String message;

    // constructor used when saving new items
    public HelloItem(String message) {
        this.message = message;
    }

    // default constructor required by Spring Data
    public HelloItem() {
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}