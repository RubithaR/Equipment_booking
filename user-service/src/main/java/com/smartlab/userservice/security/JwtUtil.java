package com.smartlab.userservice.security;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil extends com.smartlab.security.JwtUtil {

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(String email, String role, Long userId, Long facultyId, Long departmentId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        if (facultyId != null)    claims.put("facultyId", facultyId);
        if (departmentId != null) claims.put("departmentId", departmentId);
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }
}
