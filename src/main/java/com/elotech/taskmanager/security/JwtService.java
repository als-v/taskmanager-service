package com.elotech.taskmanager.security;

import com.elotech.taskmanager.config.JwtProperties;
import com.elotech.taskmanager.domain.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(JwtProperties properties) {
        this.accessKey = buildKey(properties.getSecret());
        this.refreshKey = buildKey(properties.getRefreshSecret());
        this.expirationMs = properties.getExpirationMs();
        this.refreshExpirationMs = properties.getRefreshExpirationMs();
    }

    private SecretKey buildKey(String secret) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public String generateAccessToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder().subject(user.getEmail()).claim("userId", user.getId().toString()).claim("name", user.getName()).claim("type", "access").issuedAt(new Date(now)).expiration(new Date(now + expirationMs)).signWith(accessKey).compact();
    }

    public String generateRefreshToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder().id(UUID.randomUUID().toString()).subject(user.getEmail()).claim("type", "refresh").issuedAt(new Date(now)).expiration(new Date(now + refreshExpirationMs)).signWith(refreshKey).compact();
    }

    public Jws<Claims> validateAccessToken(String token) {
        Jws<Claims> claims = Jwts.parser().verifyWith(accessKey).build().parseSignedClaims(token);
        if (!"access".equals(claims.getPayload().get("type", String.class))) {
            throw new JwtException("Not an access token");
        }
        return claims;
    }

    public Jws<Claims> parseRefreshToken(String token) {
        Jws<Claims> claims = Jwts.parser().verifyWith(refreshKey).build().parseSignedClaims(token);
        if (!"refresh".equals(claims.getPayload().get("type", String.class))) {
            throw new JwtException("Not a refresh token");
        }
        return claims;
    }

    public String extractJtiFromRefreshToken(String token) {
        return parseRefreshToken(token).getPayload().getId();
    }
}
