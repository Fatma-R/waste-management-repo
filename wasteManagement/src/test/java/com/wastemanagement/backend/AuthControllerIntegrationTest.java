package com.wastemanagement.backend;

import com.wastemanagement.backend.model.User;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test") // Uses application-test.properties
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    private final String testEmail = "safe_testuser@email.com";
    private final String testPassword = "testpass";

    @BeforeEach
    void setup() {
        // Delete only this test user if it exists
        userRepository.findByEmail(testEmail).ifPresent(userRepository::delete);
    }

    @Test
    void testSignUpSignInAndProtectedEndpoint() throws Exception {

        // 1️⃣ Ensure test user is not already in the DB
        Optional<User> existing = userRepository.findByEmail(testEmail);
        assertTrue(existing.isEmpty());

        // 2️⃣ Sign up the test user
        String signupJson = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testEmail, testPassword);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupJson))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully!"));

        // 3️⃣ Sign in to get JWT token
        String signinJson = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(testEmail, testPassword);

        String token = mockMvc.perform(post("/api/auth/signin")
                        .contentType("application/json")
                        .content(signinJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(token.startsWith("ey")); // basic check that it's a JWT

        // 4️⃣ Access public endpoint (no token needed)
        mockMvc.perform(get("/api/test/all"))
                .andExpect(status().isOk())
                .andExpect(content().string("Public Content."));

        // 5️⃣ Access protected endpoint without token -> should be unauthorized
        mockMvc.perform(get("/api/test/user"))
                .andExpect(status().isUnauthorized());

        // 6️⃣ Access protected endpoint with token -> should succeed
        mockMvc.perform(get("/api/test/user")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("User Content (JWT protected)."));  // ✅
    }
}
