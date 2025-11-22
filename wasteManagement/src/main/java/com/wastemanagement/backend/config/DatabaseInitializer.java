package com.wastemanagement.backend.config;

import com.wastemanagement.backend.model.user.Admin;
import com.wastemanagement.backend.model.user.ERole;
import com.wastemanagement.backend.model.user.Role;
import com.wastemanagement.backend.repository.RoleRepository;
import com.wastemanagement.backend.repository.user.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final RoleRepository roleRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            // --- Initialize roles ---
            if (!roleRepository.existsByName(ERole.ROLE_USER)) {
                roleRepository.save(new Role(ERole.ROLE_USER));
                System.out.println("Created ROLE_USER");
            }

            if (!roleRepository.existsByName(ERole.ROLE_ADMIN)) {
                roleRepository.save(new Role(ERole.ROLE_ADMIN));
                System.out.println("Created ROLE_ADMIN");
            }

            // --- Initialize admin ---
            if (adminRepository.count() == 0) {
                Admin admin = new Admin();
                admin.setFullName("Super Admin");
                admin.setEmail("admin@example.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                adminRepository.save(admin);
                System.out.println("Initial admin created: admin@example.com / admin123");
            }
        };
    }
}
