package com.smartlab.userservice.security;

import com.smartlab.security.JwtAuthFilter;
import com.smartlab.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] ADMIN_ROLES = {Roles.MAIN_ADMIN, Roles.DEPT_ADMIN};

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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
                .requestMatchers("/api/users/register", "/api/users/login", "/api/users/check-availability").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/faculties/**", "/api/departments/**").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/departments/*/hod").hasAnyRole(ADMIN_ROLES)
                .requestMatchers("/api/users/instructors/pending").hasAnyRole(ADMIN_ROLES)
                .requestMatchers(HttpMethod.PATCH, "/api/users/*/approve").hasAnyRole(ADMIN_ROLES)
                .requestMatchers(HttpMethod.DELETE, "/api/users/*/reject").hasAnyRole(ADMIN_ROLES)
                .requestMatchers("/api/users/by-role/**").hasAnyRole(ADMIN_ROLES)
                .requestMatchers(HttpMethod.GET, "/api/users").hasAnyRole(ADMIN_ROLES)
                .requestMatchers(HttpMethod.GET, "/api/users/search").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/users/*").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
