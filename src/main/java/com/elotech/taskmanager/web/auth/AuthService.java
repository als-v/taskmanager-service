package com.elotech.taskmanager.web.auth;

import com.elotech.taskmanager.common.error.ConflictException;
import com.elotech.taskmanager.common.error.UnauthorizedException;
import com.elotech.taskmanager.domain.entities.User;
import com.elotech.taskmanager.repository.UserRepository;
import com.elotech.taskmanager.security.JwtService;
import com.elotech.taskmanager.security.RefreshTokenStore;
import com.elotech.taskmanager.web.auth.dto.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService, RefreshTokenStore refreshTokenStore) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
    }

    public UserResponse signUp(SignUpRequest request) {

        if (userRepository.findByEmail(request.email()).isPresent())
            throw new ConflictException("error.auth.email-in-use", "Email already in use: " + request.email());

        User user = User.builder().name(request.name()).email(request.email()).password(passwordEncoder.encode(request.password())).build();

        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    public AuthResponse login(LoginRequest request) {

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("error.auth.invalid-credentials", "Invalid email or password");
        }

        User user = userRepository.findByEmail(request.email()).orElseThrow(() -> new UnauthorizedException("error.auth.invalid-credentials", "Invalid email or password"));

        return issueTokens(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        Jws<Claims> claims;

        try {
            claims = jwtService.parseRefreshToken(request.refreshToken());
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException("error.auth.refresh-expired", "Refresh token expired");
        } catch (JwtException e) {
            throw new UnauthorizedException("error.auth.refresh-invalid", "Invalid refresh token");
        }

        String jti = claims.getPayload().getId();

        if (!refreshTokenStore.exists(jti))
            throw new UnauthorizedException("error.auth.refresh-revoked", "Invalid refresh token");

        String email = claims.getPayload().getSubject();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UnauthorizedException("error.auth.refresh-invalid", "Invalid refresh token"));

        AuthResponse response = issueTokens(user);
        refreshTokenStore.delete(jti);
        return response;
    }

    public void logout(RefreshRequest request) {

        try {
            String jti = jwtService.extractJtiFromRefreshToken(request.refreshToken());
            refreshTokenStore.delete(jti);
        } catch (JwtException ignored) {
        }
    }

    private AuthResponse issueTokens(User user) {

        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        String jti = jwtService.extractJtiFromRefreshToken(refresh);
        refreshTokenStore.save(jti, user.getId(), Duration.ofMillis(jwtService.getRefreshExpirationMs()));

        return new AuthResponse(access, refresh, "Bearer", UserResponse.from(user));
    }
}
