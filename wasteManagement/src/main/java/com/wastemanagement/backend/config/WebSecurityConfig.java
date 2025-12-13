package com.wastemanagement.backend.config;

import com.wastemanagement.backend.security.AuthEntryPointJwt;
import com.wastemanagement.backend.security.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    private final UserDetailsService userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    public WebSecurityConfig(UserDetailsService userDetailsService,
                             AuthEntryPointJwt unauthorizedHandler) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse allowed origins from property (comma-separated)
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/signin").permitAll()
                        .requestMatchers("/api/v1/auth/signup").hasRole("ADMIN")
                        .requestMatchers("/api/test/all").permitAll()
                        .requestMatchers("/api/v1/hello").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // Swagger/API docs
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Employee CRUD -> Admin ONLY
                        .requestMatchers(HttpMethod.POST, "/api/v1/employees/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/employees/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/employees/**").hasRole("ADMIN")

                        // Depot CRUD -> Admin ONLY
                        .requestMatchers(HttpMethod.POST, "/api/v1/depots/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/depots/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/depots/**").hasRole("ADMIN")
                        //.requestMatchers(HttpMethod.GET, "/api/v1/depots/**").hasRole("ADMIN")

                        // Collection point CRUD -> Admin ONLY
                        .requestMatchers(HttpMethod.POST, "/api/v1/collectionPoints/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/collectionPoints/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/collectionPoints/**").hasRole("ADMIN")
                        //.requestMatchers(HttpMethod.GET, "/api/v1/collectionPoints/**").hasRole("ADMIN")

                        // Bins CRUD -> Admin ONLY
                        .requestMatchers(HttpMethod.POST, "/api/v1/bins/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bins/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/bins/**").hasRole("ADMIN")
                        //.requestMatchers(HttpMethod.GET, "/api/v1/bins/**").hasRole("ADMIN")

                        // Bins CRUD -> Admin ONLY
                        .requestMatchers(HttpMethod.POST, "/api/v1/bin-readings/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bin-readings/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/bin-readings/**").hasRole("ADMIN")
                        //.requestMatchers(HttpMethod.GET, "/api/v1/bin-readings/**").hasRole("ADMIN")

                        // Admin CRUD -> Admin ONLY
                        .requestMatchers(HttpMethod.POST, "/api/v1/admins/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admins/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admins/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admins/**").hasRole("ADMIN")

                        // assignment CRUD -> Admin ONLY
                        .requestMatchers(HttpMethod.POST, "/api/v1/tournee-assignments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/tournee-assignments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/tournee-assignments/**").hasRole("ADMIN")
                        //.requestMatchers(HttpMethod.GET, "/api/v1/tournee-assignments/**").hasRole("ADMIN")

                        // Tournee CRUD -> Admin ONLY
                        .requestMatchers(HttpMethod.POST, "/api/v1/tournees/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/tournees/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/tournees/**").hasRole("ADMIN")
                        //.requestMatchers(HttpMethod.GET, "/api/v1/tournees/**").hasRole("ADMIN")
                        //.requestMatchers(HttpMethod.GET, "/api/v1/tournee-assignments/employee/**").hasRole("USER")

                        // Vehicle CRUD -> Admin ONLY
                        .requestMatchers(HttpMethod.POST, "/api/v1/vehicles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/vehicles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/vehicles/**").hasRole("ADMIN")
                        // .requestMatchers(HttpMethod.GET, "/api/v1/vehicles/**").hasRole("ADMIN")
                        // CollectionPoint CRUD -> Admin ONLY
                        .requestMatchers(HttpMethod.POST, "/api/v1/collectionPoints/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/collectionPoints/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/collectionPoints/**").hasRole("ADMIN")
                        // Alert CRUD -> Admin ONLY
                        .requestMatchers(HttpMethod.POST, "/api/v1/alerts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/alerts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/alerts/**").hasRole("ADMIN")

                        // Mode CRUD -> Admin ONLY
                        .requestMatchers(HttpMethod.POST, "/api/v1/auto-planning/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/auto-planning/**").hasRole("ADMIN")




                        // RouteStep CRUD -> Admin ONLY
                        .requestMatchers(HttpMethod.POST, "/api/v1/route-steps/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/route-steps/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/route-steps/**").hasRole("ADMIN")
                        //.requestMatchers(HttpMethod.GET, "/api/v1/route-steps/**").hasRole("ADMIN")

                        // Employee READ -> Admin ONLY (employees cannot view other employees)
                        //.requestMatchers(HttpMethod.GET, "/api/v1/employees/**").hasRole("ADMIN")

                        //When employee wants to view his own profile. To be created later.
                        //.requestMatchers(HttpMethod.GET, "/api/v1/employees/me").hasAnyRole("ADMIN","USER")

                        // Anything else must be authenticated

                        .anyRequest().authenticated()

                )
                .authenticationProvider(authenticationProvider());


        // Add JWT filter before username/password authentication
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}