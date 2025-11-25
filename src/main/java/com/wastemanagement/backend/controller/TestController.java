package com.wastemanagement.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/test")
public class TestController {

    // Public endpoint — no authentication required
    @GetMapping("/all")
    public String allAccess() {
        return "Public Content.";
    }

    // Protected endpoint — requires authentication (JWT)
    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public String userAccess() {
        return "User Content (JWT protected).";
    }
}
