package com.wastemanagement.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wastemanagement.backend.model.user.ERole;
import com.wastemanagement.backend.model.user.Role;
import com.wastemanagement.backend.model.user.User;
import com.wastemanagement.backend.repository.RoleRepository;
import com.wastemanagement.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private ObjectMapper objectMapper;

    private final String testUserEmail = "test_user@email.com";
    private final String testAdminEmail = "test_admin@email.com";
    private final String testPassword = "testpass123";

    @BeforeEach
    void setup() {
        // Initialize roles for test database (in case they don't exist)
        if (!roleRepository.existsByName(ERole.ROLE_USER)) {
            roleRepository.save(new Role(ERole.ROLE_USER));
        }
        if (!roleRepository.existsByName(ERole.ROLE_ADMIN)) {
            roleRepository.save(new Role(ERole.ROLE_ADMIN));
        }

        // Clean up test users
        userRepository.findByEmail(testUserEmail).ifPresent(userRepository::delete);
        userRepository.findByEmail(testAdminEmail).ifPresent(userRepository::delete);
    }

    @Test
    void testUserSignUpSignInAndProtectedEndpoints() throws Exception {
        // 1️⃣ Ensure test user doesn't exist
        Optional<User> existing = userRepository.findByEmail(testUserEmail);
        assertTrue(existing.isEmpty(), "Test user should not exist before test");

        // 2️⃣ Sign up regular user (no roles specified = default ROLE_USER)
        String signupJson = """
                {
                    "email": "%s",
                    "password": "%s",
                    "fullName": "Test User"
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

        // 3️⃣ Verify user was created with ROLE_USER
        User createdUser = userRepository.findByEmail(testUserEmail).orElseThrow();
        assertNotNull(createdUser);
        assertEquals(1, createdUser.getRoles().size());
        assertTrue(createdUser.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_USER));

        // 4️⃣ Sign in to get JWT token
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

        // Parse JWT response
        JsonNode signinNode = objectMapper.readTree(signinResponse);
        String token = signinNode.get("token").asText();
        String email = signinNode.get("email").asText();
        String message = signinNode.get("message").asText();

        assertTrue(token.startsWith("ey"), "Token should be a valid JWT");
        assertEquals(testUserEmail, email);
        assertEquals("Login successful", message);

        // Verify roles in response
        JsonNode rolesNode = signinNode.get("roles");
        assertNotNull(rolesNode);
        assertTrue(rolesNode.isArray());
        assertEquals(1, rolesNode.size());
        assertEquals("ROLE_USER", rolesNode.get(0).asText());

        // 5️⃣ Access public endpoint (no token needed)
        mockMvc.perform(get("/api/test/all"))
                .andExpect(status().isOk())
                .andExpect(content().string("Public Content."));

        // 6️⃣ Access protected endpoint without token -> should be unauthorized
        mockMvc.perform(get("/api/test/user"))
                .andExpect(status().isUnauthorized());

        // 7️⃣ Access protected endpoint with token -> should succeed
        mockMvc.perform(get("/api/test/user")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("User Content (JWT protected)."));
    }

    @Test
    void testAdminSignUpAndAccess() throws Exception {
        // 1️⃣ Sign up admin user (with admin role)
        String signupJson = """
                {
                    "email": "%s",
                    "password": "%s",
                    "fullName": "Test Admin",
                    "roles": ["admin", "user"]
                }
                """.formatted(testAdminEmail, testPassword);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(signupJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        // 2️⃣ Verify user has both ADMIN and USER roles
        User adminUser = userRepository.findByEmail(testAdminEmail).orElseThrow();
        assertEquals(2, adminUser.getRoles().size());
        assertTrue(adminUser.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_ADMIN));
        assertTrue(adminUser.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_USER));

        // 3️⃣ Sign in as admin
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

        // Verify both roles are in the response
        JsonNode rolesNode = signinNode.get("roles");
        assertEquals(2, rolesNode.size());
        assertTrue(rolesNode.toString().contains("ROLE_ADMIN"));
        assertTrue(rolesNode.toString().contains("ROLE_USER"));

        // 4️⃣ Admin can access user endpoints
        mockMvc.perform(get("/api/test/user")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testSignUpWithExistingEmail() throws Exception {
        // 1️⃣ Create first user
        String signupJson = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testUserEmail, testPassword);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(signupJson))
                .andExpect(status().isCreated());

        // 2️⃣ Try to create another user with same email
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(signupJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email is already in use"));
    }

    @Test
    void testSignInWithInvalidCredentials() throws Exception {
        // 1️⃣ Create user
        String signupJson = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testUserEmail, testPassword);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType("application/json")
                        .content(signupJson))
                .andExpect(status().isCreated());

        // 2️⃣ Try to sign in with wrong password
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
        // 1️⃣ Test missing email
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

        // 2️⃣ Test missing password
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