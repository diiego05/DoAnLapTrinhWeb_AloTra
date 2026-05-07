package com.alotra.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secret;

    @Value("${security.jwt.expiration-time}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ✅ Hàm generate dùng Map claims
    public String generate(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(subject)
                .claims(claims) // chứa id, role, name, ...
                .issuedAt(Date.from(now))
                .expiration(new Date(now.toEpochMilli() + expirationMs))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    // ✅ Lấy subject (email)
    public String extractSubject(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // ✅ Lấy role (nếu cần dùng trong filter)
    public String extractRole(String token) {
        Object role = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role");
        return role != null ? role.toString() : null;
    }
}
