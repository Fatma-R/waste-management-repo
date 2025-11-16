package com.wastemanagement.backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/hello")
@RestController
public class HelloController {

    @GetMapping
    public String hello() {
        return "Hello World";
    }
}
