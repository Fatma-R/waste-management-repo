package com.wastemanagement.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/v1/hello")
@RestController
public class HelloController {

    @GetMapping
    public String hello() {
        return "Hello World";
    }

    @PostMapping()
    public ResponseEntity<?> helloPost(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "message", "Hello (POST) received",
                "received", body == null ? Map.of() : body,
                "timestamp", Instant.now().toString()
        ));
    }
}
