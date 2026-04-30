package com.smartlab.equipmentservice.security;

import com.smartlab.security.JwtAuthFilter;
import com.smartlab.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] ADMIN_ROLES        = {Roles.MAIN_ADMIN, Roles.DEPT_ADMIN};
    private static final String[] LAB_MANAGER_ROLES  = {Roles.MAIN_ADMIN, Roles.DEPT_ADMIN, Roles.INSTRUCTOR};

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Inter-service Feign calls hit these without a JWT — booking-service relies on them.
                // Keep them open until Phase 6 introduces service-to-service auth.
                .requestMatchers(HttpMethod.GET,    "/api/items/*").permitAll()
                .requestMatchers(HttpMethod.PATCH,  "/api/items/*/status").permitAll()
                .requestMatchers(HttpMethod.GET,    "/api/labs/*").permitAll()

                .requestMatchers(HttpMethod.POST,   "/api/labs").hasAnyRole(ADMIN_ROLES)
                .requestMatchers(HttpMethod.PUT,    "/api/labs/*").hasAnyRole(ADMIN_ROLES)
                .requestMatchers(HttpMethod.PATCH,  "/api/labs/*/instructor").hasAnyRole(ADMIN_ROLES)
                .requestMatchers(HttpMethod.DELETE, "/api/labs/*").hasAnyRole(ADMIN_ROLES)

                .requestMatchers(HttpMethod.POST,   "/api/items").hasAnyRole(LAB_MANAGER_ROLES)
                .requestMatchers(HttpMethod.PUT,    "/api/items/*").hasAnyRole(LAB_MANAGER_ROLES)
                .requestMatchers(HttpMethod.DELETE, "/api/items/*").hasAnyRole(LAB_MANAGER_ROLES)

                .requestMatchers(HttpMethod.GET, "/api/items/**", "/api/labs/**").authenticated()

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
