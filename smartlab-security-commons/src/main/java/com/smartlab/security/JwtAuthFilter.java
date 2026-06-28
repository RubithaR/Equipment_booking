package com.smartlab.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtUtil.isTokenValid(token)) {
                    Claims c = jwtUtil.extractClaims(token);
                    Long userId       = c.get("userId", Number.class) != null
                            ? ((Number) c.get("userId")).longValue() : null;
                    String role       = c.get("role", String.class);
                    Long facultyId    = c.get("facultyId", Number.class) != null
                            ? ((Number) c.get("facultyId")).longValue() : null;
                    Long departmentId = c.get("departmentId", Number.class) != null
                            ? ((Number) c.get("departmentId")).longValue() : null;
                    UserContext ctx = new UserContext(userId, c.getSubject(), role, facultyId, departmentId);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            ctx, null,
                            role != null ? List.of(new SimpleGrantedAuthority("ROLE_" + role)) : List.of());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                logger.warn("JWT validation error: " + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
