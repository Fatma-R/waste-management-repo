package com.wastemanagement.backend.config;

import com.wastemanagement.backend.model.ERole;
import com.wastemanagement.backend.model.Role;
import com.wastemanagement.backend.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseInitializer {

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            // Create default roles if they don't exist
            if (!roleRepository.existsByName(ERole.ROLE_USER)) {
                roleRepository.save(new Role(ERole.ROLE_USER));
                System.out.println("Created ROLE_USER");
            }

            if (!roleRepository.existsByName(ERole.ROLE_ADMIN)) {
                roleRepository.save(new Role(ERole.ROLE_ADMIN));
                System.out.println("Created ROLE_ADMIN");
            }
        };
    }
}