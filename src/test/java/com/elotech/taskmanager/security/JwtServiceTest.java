package com.elotech.taskmanager.security;

import com.elotech.taskmanager.config.JwtProperties;
import com.elotech.taskmanager.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-access-secret-key-0123456789-not-real");
        properties.setRefreshSecret("test-refresh-secret-key-0123456789-not-real");
        properties.setExpirationMs(60_000);
        properties.setRefreshExpirationMs(120_000);
        jwtService = new JwtService(properties);

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Maria Silva")
                .email("maria@exemplo.com")
                .password("hash")
                .build();
    }

    @Test
    void generateAccessToken_shouldContainExpectedClaims() {
        String token = jwtService.generateAccessToken(user);

        Jws<Claims> jws = jwtService.validateAccessToken(token);
        Claims claims = jws.getPayload();

        assertThat(claims.getSubject()).isEqualTo(user.getEmail());
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.get("userId", String.class)).isEqualTo(user.getId().toString());
        assertThat(claims.get("name", String.class)).isEqualTo(user.getName());
    }

    @Test
    void generateRefreshToken_shouldContainTypeAndUniqueJti() {
        String tokenA = jwtService.generateRefreshToken(user);
        String tokenB = jwtService.generateRefreshToken(user);

        Claims claimsA = jwtService.parseRefreshToken(tokenA).getPayload();
        Claims claimsB = jwtService.parseRefreshToken(tokenB).getPayload();

        assertThat(claimsA.get("type", String.class)).isEqualTo("refresh");
        assertThat(claimsA.getId()).isNotBlank();
        assertThat(claimsA.getId()).isNotEqualTo(claimsB.getId());
    }

    @Test
    void validateAccessToken_shouldRejectRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(user);

        assertThatThrownBy(() -> jwtService.validateAccessToken(refreshToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAccessToken_shouldThrowExpiredJwtException_whenTokenExpired() {
        JwtProperties expiredProperties = new JwtProperties();
        expiredProperties.setSecret("test-access-secret-key-0123456789-not-real");
        expiredProperties.setRefreshSecret("test-refresh-secret-key-0123456789-not-real");
        expiredProperties.setExpirationMs(-10_000);
        expiredProperties.setRefreshExpirationMs(120_000);
        JwtService expiredJwtService = new JwtService(expiredProperties);

        String expiredToken = expiredJwtService.generateAccessToken(user);

        assertThatThrownBy(() -> expiredJwtService.validateAccessToken(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void validateAccessToken_shouldRejectTamperedSignature() {
        JwtProperties otherProperties = new JwtProperties();
        otherProperties.setSecret("another-completely-different-secret-key-123");
        otherProperties.setRefreshSecret("another-completely-different-refresh-key-123");
        otherProperties.setExpirationMs(60_000);
        otherProperties.setRefreshExpirationMs(120_000);
        JwtService otherJwtService = new JwtService(otherProperties);

        String tokenSignedByOther = otherJwtService.generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.validateAccessToken(tokenSignedByOther))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseRefreshToken_shouldRejectAccessToken() {
        String accessToken = jwtService.generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseRefreshToken(accessToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseRefreshToken_shouldThrowExpiredJwtException_whenTokenExpired() {
        JwtProperties expiredProperties = new JwtProperties();
        expiredProperties.setSecret("test-access-secret-key-0123456789-not-real");
        expiredProperties.setRefreshSecret("test-refresh-secret-key-0123456789-not-real");
        expiredProperties.setExpirationMs(60_000);
        expiredProperties.setRefreshExpirationMs(-10_000);
        JwtService expiredJwtService = new JwtService(expiredProperties);

        String expiredToken = expiredJwtService.generateRefreshToken(user);

        assertThatThrownBy(() -> expiredJwtService.parseRefreshToken(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void extractJtiFromRefreshToken_shouldReturnJtiFromToken() {
        String refreshToken = jwtService.generateRefreshToken(user);

        String jti = jwtService.extractJtiFromRefreshToken(refreshToken);

        assertThat(jti).isEqualTo(jwtService.parseRefreshToken(refreshToken).getPayload().getId());
    }

    @Test
    void getRefreshExpirationMs_shouldReturnConfiguredValue() {
        assertThat(jwtService.getRefreshExpirationMs()).isEqualTo(120_000);
    }
}
