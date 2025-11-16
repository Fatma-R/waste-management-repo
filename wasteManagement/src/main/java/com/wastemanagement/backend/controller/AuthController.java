package com.wastemanagement.backend.controller;

import com.wastemanagement.backend.model.User;
import com.wastemanagement.backend.repository.UserRepository;
import com.wastemanagement.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtil jwtUtils;

    // Login endpoint
    @PostMapping("/signin")
    public String authenticateUser(@RequestBody User user) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),   // Use email as username
                        user.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return jwtUtils.generateToken(userDetails.getUsername());
    }

    // Registration endpoint
    @PostMapping("/signup")
    public String registerUser(@RequestBody User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return "Error: Email is already in use!";
        }

        // Create new user's account
        User newUser = new User();
        newUser.setFullName(user.getFullName());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(encoder.encode(user.getPassword()));
        userRepository.save(newUser);

        return "User registered successfully!";
    }
}
