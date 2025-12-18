package com.wastemanagement.backend.config;

import com.wastemanagement.backend.model.GeoJSONPoint;
import com.wastemanagement.backend.model.tournee.Depot;
import com.wastemanagement.backend.model.user.Admin;
import com.wastemanagement.backend.model.user.ERole;
import com.wastemanagement.backend.model.user.Role;
import com.wastemanagement.backend.repository.RoleRepository;
import com.wastemanagement.backend.repository.UserRepository;
import com.wastemanagement.backend.repository.tournee.DepotRepository;
import com.wastemanagement.backend.repository.user.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.wastemanagement.backend.model.user.User;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final RoleRepository roleRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepotRepository depotRepository;


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
                // create user
                User user = new User();
                user.setFullName("Super Admin");
                user.setEmail("admin@example.com");
                user.setPassword(passwordEncoder.encode("admin123"));

                Set<Role> roles = new HashSet<>();
                Role roleAdmin = roleRepository.findByName(ERole.ROLE_ADMIN)
                        .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));
                Role roleUser = roleRepository.findByName(ERole.ROLE_USER)
                        .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
                roles.add(roleAdmin);
                roles.add(roleUser);
                user.setRoles(roles);

                user = userRepository.save(user);
                // Create admin linked to user
                Admin admin = new Admin();
                admin.setUser(user);

                adminRepository.save(admin);
                System.out.println("Initial admin created: admin@example.com / admin123");
            }

            // --- Initialize main depot ---
            if (!depotRepository.existsById("MAIN_DEPOT_ID")) {
                GeoJSONPoint location = new GeoJSONPoint();
                location.setType("Point");
                location.setCoordinates(new double[]{10.19, 36.8});

                Depot mainDepot = new Depot();
                mainDepot.setId("MAIN_DEPOT");
                mainDepot.setName("Main depot");
                mainDepot.setAddress("Rue X, Tunis");
                mainDepot.setLocation(location);

                depotRepository.save(mainDepot);
                System.out.println("Created MAIN_DEPOT");
            }
    };}
}