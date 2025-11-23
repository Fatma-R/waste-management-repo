package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.model.user.Admin;
import com.wastemanagement.backend.model.user.Employee;
import com.wastemanagement.backend.model.user.ERole;
import com.wastemanagement.backend.model.user.Role;
import com.wastemanagement.backend.model.user.Skill;
import com.wastemanagement.backend.model.user.User;
import com.wastemanagement.backend.repository.RoleRepository;
import com.wastemanagement.backend.repository.UserRepository;
import com.wastemanagement.backend.repository.user.AdminRepository;
import com.wastemanagement.backend.repository.user.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final String testUserEmail = "test_user@email.com";
    private final String testAdminEmail = "test_admin@email.com";
    private final String testPassword = "testpass123";

    @BeforeEach
    void setup() {
        // Ensure roles exist
        if (!roleRepository.existsByName(ERole.ROLE_USER)) {
            roleRepository.save(new Role(ERole.ROLE_USER));
        }
        if (!roleRepository.existsByName(ERole.ROLE_ADMIN)) {
            roleRepository.save(new Role(ERole.ROLE_ADMIN));
        }

        // Clean test users if they already exist
        userRepository.findByEmail(testUserEmail).ifPresent(user -> {
            employeeRepository.findByUser(user).ifPresent(employeeRepository::delete);
            adminRepository.findByUser(user).ifPresent(adminRepository::delete);
            userRepository.delete(user);
        });

        userRepository.findByEmail(testAdminEmail).ifPresent(user -> {
            employeeRepository.findByUser(user).ifPresent(employeeRepository::delete);
            adminRepository.findByUser(user).ifPresent(adminRepository::delete);
            userRepository.delete(user);
        });
    }

    @Test
    void testUserSignUpSignInAndEmployeeCreation() throws Exception {
        Optional<User> existing = userRepository.findByEmail(testUserEmail);
        assertTrue(existing.isEmpty(), "Test user should not exist before test");

        // 1️⃣ signup user (no explicit roles -> ROLE_USER, Employee created)
        String signupJson = """
                {
                    "email": "%s",
                    "password": "%s",
                    "fullName": "Test User",
                    "skill": "DRIVER"
                }
                """.formatted(testUserEmail, testPassword);

        String signupResponse = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(signupJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode signupNode = objectMapper.readTree(signupResponse);
        assertEquals("User registered successfully", signupNode.get("message").asText());

        // 2️⃣ User created with ROLE_USER
        User createdUser = userRepository.findByEmail(testUserEmail).orElseThrow();
        assertNotNull(createdUser);
        assertEquals(1, createdUser.getRoles().size());
        assertTrue(createdUser.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_USER));

        // 3️⃣ Employee linked to User
        Employee employee = employeeRepository.findByUser(createdUser)
                .orElseThrow(() -> new AssertionError("Employee should be created for user signup"));
        assertEquals(Skill.DRIVER, employee.getSkill());

        // 4️⃣ signin to get JWT (we still check payload, even if security is disabled)
        String signinJson = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testUserEmail, testPassword);

        String signinResponse = mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType("application/json")
                        .content(signinJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode signinNode = objectMapper.readTree(signinResponse);
        String token = signinNode.get("token").asText();
        String email = signinNode.get("email").asText();
        String message = signinNode.get("message").asText();

        assertNotNull(token);
        assertTrue(token.length() > 10);
        assertEquals(testUserEmail, email);
        assertEquals("Login successful", message);

        JsonNode rolesNode = signinNode.get("roles");
        assertNotNull(rolesNode);
        assertTrue(rolesNode.isArray());
        assertEquals(1, rolesNode.size());
        assertEquals("ROLE_USER", rolesNode.get(0).asText());

        // ⚠️ Security is disabled, so /api/test/user will not return 401.
        // If you still want, you can keep a simple sanity check like:
        mockMvc.perform(get("/api/test/all"))
                .andExpect(status().isOk());
    }

    @Test
    void testAdminSignUpCreatesAdminAndEmployeeAndAccess() throws Exception {
        // 1️⃣ signup admin with roles ["admin","user"]
        String signupJson = """
                {
                    "email": "%s",
                    "password": "%s",
                    "fullName": "Test Admin",
                    "roles": ["admin", "user"],
                    "skill": "AGENT"
                }
                """.formatted(testAdminEmail, testPassword);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(signupJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        // 2️⃣ User with 2 roles
        User adminUser = userRepository.findByEmail(testAdminEmail).orElseThrow();
        assertEquals(2, adminUser.getRoles().size());
        assertTrue(adminUser.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_ADMIN));
        assertTrue(adminUser.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_USER));

        // 3️⃣ Admin and Employee linked to User
        Admin admin = adminRepository.findByUser(adminUser)
                .orElseThrow(() -> new AssertionError("Admin should be created for admin signup"));
        assertNotNull(admin);

        Employee employee = employeeRepository.findByUser(adminUser)
                .orElseThrow(() -> new AssertionError("Employee should be created for admin signup"));
        assertEquals(Skill.AGENT, employee.getSkill());

        // 4️⃣ signin admin (we still validate response)
        String signinJson = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testAdminEmail, testPassword);

        String signinResponse = mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType("application/json")
                        .content(signinJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode signinNode = objectMapper.readTree(signinResponse);
        String adminToken = signinNode.get("token").asText();
        assertNotNull(adminToken);

        JsonNode rolesNode = signinNode.get("roles");
        assertEquals(2, rolesNode.size());
        assertTrue(rolesNode.toString().contains("ROLE_ADMIN"));
        assertTrue(rolesNode.toString().contains("ROLE_USER"));

        // Security is disabled here, so we don't test access control with/without token.
    }

    @Test
    void testSignUpWithExistingEmail() throws Exception {
        String signupJson = """
                {
                    "email": "%s",
                    "password": "%s",
                    "fullName": "Dup User"
                }
                """.formatted(testUserEmail, testPassword);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(signupJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(signupJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email is already in use"));
    }

    @Test
    void testSignInWithInvalidCredentials() throws Exception {
        String signupJson = """
                {
                    "email": "%s",
                    "password": "%s",
                    "fullName": "User For Login Test"
                }
                """.formatted(testUserEmail, testPassword);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(signupJson))
                .andExpect(status().isCreated());

        String wrongPasswordJson = """
                {
                    "email": "%s",
                    "password": "wrongpassword"
                }
                """.formatted(testUserEmail);

        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType("application/json")
                        .content(wrongPasswordJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void testSignUpValidation() throws Exception {
        String noEmailJson = """
                {
                    "password": "test123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(noEmailJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is required"));

        String noPasswordJson = """
                {
                    "email": "test@test.com"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(noPasswordJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password is required"));
    }
}
