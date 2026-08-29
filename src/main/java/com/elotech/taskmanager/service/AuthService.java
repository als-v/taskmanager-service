package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.error.ConflictException;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.domain.error.UnauthorizedException;
import com.elotech.taskmanager.domain.dto.request.auth.LoginRequest;
import com.elotech.taskmanager.domain.dto.request.auth.RefreshRequest;
import com.elotech.taskmanager.domain.dto.request.auth.SignUpRequest;
import com.elotech.taskmanager.domain.dto.response.auth.AuthResponse;
import com.elotech.taskmanager.domain.dto.response.auth.UserResponse;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.repository.UserRepository;
import com.elotech.taskmanager.security.JwtService;
import com.elotech.taskmanager.security.RefreshTokenStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

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
            throw new ConflictException(ErrorMessages.AUTH_EMAIL_IN_USE_CODE, ErrorMessages.AUTH_EMAIL_IN_USE_MESSAGE + request.email());

        User user = User.builder().name(request.name()).email(request.email()).password(passwordEncoder.encode(request.password())).build();

        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    public AuthResponse login(LoginRequest request) {

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            throw new UnauthorizedException(ErrorMessages.AUTH_INVALID_CREDENTIALS_CODE, ErrorMessages.AUTH_INVALID_CREDENTIALS_MESSAGE);
        }

        User user = userRepository.findByEmail(request.email()).orElseThrow(() -> new UnauthorizedException(ErrorMessages.AUTH_INVALID_CREDENTIALS_CODE, ErrorMessages.AUTH_INVALID_CREDENTIALS_MESSAGE));

        return issueTokens(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        Jws<Claims> claims;

        try {
            claims = jwtService.parseRefreshToken(request.refreshToken());
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException(ErrorMessages.AUTH_REFRESH_EXPIRED_CODE, ErrorMessages.AUTH_REFRESH_EXPIRED_MESSAGE);
        } catch (JwtException e) {
            throw new UnauthorizedException(ErrorMessages.AUTH_REFRESH_INVALID_CODE, ErrorMessages.AUTH_INVALID_REFRESH_TOKEN_MESSAGE);
        }

        String jti = claims.getPayload().getId();

        if (!refreshTokenStore.exists(jti))
            throw new UnauthorizedException(ErrorMessages.AUTH_REFRESH_REVOKED_CODE, ErrorMessages.AUTH_INVALID_REFRESH_TOKEN_MESSAGE);

        String email = claims.getPayload().getSubject();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UnauthorizedException(ErrorMessages.AUTH_REFRESH_INVALID_CODE, ErrorMessages.AUTH_INVALID_REFRESH_TOKEN_MESSAGE));

        AuthResponse response = issueTokens(user);
        refreshTokenStore.delete(jti);
        return response;
    }

    public void logout(RefreshRequest request) {

        try {
            String jti = jwtService.extractJtiFromRefreshToken(request.refreshToken());
            refreshTokenStore.delete(jti);
        } catch (JwtException e) {
            log.debug("Token inválido/expirado");
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
