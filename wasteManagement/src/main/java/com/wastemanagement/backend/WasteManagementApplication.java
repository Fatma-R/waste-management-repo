package com.wastemanagement.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WasteManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(WasteManagementApplication.class, args);
    }

}
