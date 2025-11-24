package com.wastemanagement.backend.controller;

import com.wastemanagement.backend.dto.request.LoginRequest;
import com.wastemanagement.backend.dto.request.SignupRequest;
import com.wastemanagement.backend.dto.response.ErrorResponse;
import com.wastemanagement.backend.dto.response.JwtResponse;
import com.wastemanagement.backend.dto.response.MessageResponse;
import com.wastemanagement.backend.model.user.ERole;
import com.wastemanagement.backend.model.user.Role;
import com.wastemanagement.backend.model.user.User;
import com.wastemanagement.backend.service.user.AdminService;
import com.wastemanagement.backend.service.user.EmployeeService;
import com.wastemanagement.backend.repository.RoleRepository;
import com.wastemanagement.backend.repository.UserRepository;
import com.wastemanagement.backend.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtils;
    private final AdminService adminService;
    private final EmployeeService employeeService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder encoder,
                          JwtUtil jwtUtils,
                          AdminService adminService,
                          EmployeeService employeeService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.adminService = adminService;
        this.employeeService = employeeService;
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwt = jwtUtils.generateToken(userDetails.getUsername());

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new JwtResponse(
                    jwt,
                    userDetails.getUsername(),
                    roles,
                    "Login successful"
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid email or password"));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Account is disabled"));
        } catch (LockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Account is locked"));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Authentication failed"));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signupRequest) {

        if (signupRequest.getEmail() == null || signupRequest.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Email is required"));
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Email is already in use"));
        }

        // 🔐 1) Determine password
        String rawPassword;

        if (signupRequest.getPassword() == null || signupRequest.getPassword().trim().isEmpty()) {
            // Auto-generate backend password
            rawPassword = generateRandomPassword();

            // DEV OVERRIDE: comment when pushing to production
            rawPassword = "123";  // ⚠️ DEV ONLY — static password to simplify tests

            System.out.println("Generated password for " + signupRequest.getEmail() + ": " + rawPassword);
        } else {
            rawPassword = signupRequest.getPassword();
        }

        // 2️⃣ Create User
        User user = new User();
        user.setFullName(signupRequest.getFullName());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(encoder.encode(rawPassword));

        // 3️⃣ Assign Roles
        Set<Role> roles = new HashSet<>();
        Set<String> requestedRoles = signupRequest.getRoles();

        if (requestedRoles == null || requestedRoles.isEmpty()) {
            roles.add(roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("ROLE_USER not found")));
        } else {
            for (String roleName : requestedRoles) {
                switch (roleName.toLowerCase()) {
                    case "admin":
                        roles.add(roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found")));
                        roles.add(roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("ROLE_USER not found")));
                        break;
                    default:
                        roles.add(roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("ROLE_USER not found")));
                }
            }
        }

        user.setRoles(roles);
        user = userRepository.save(user);

        // 4️⃣ Create the associated profile
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            employeeService.createFromUser(user, signupRequest.getSkill());
        } else {
            if (requestedRoles.stream().anyMatch(r -> r.equalsIgnoreCase("admin"))) {
                adminService.createFromUser(user);
            }
            if (requestedRoles.stream().anyMatch(r -> r.equalsIgnoreCase("user"))) {
                employeeService.createFromUser(user, signupRequest.getSkill());
            }
        }

        // Returning password ONLY FOR TEST — will be removed later
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");

        // ⚠️ DEV ONLY — allows front-end to log in during development
        response.put("generatedPassword", rawPassword);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    private String generateRandomPassword() {
        return UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 10); // 10-char random password
    }



}
