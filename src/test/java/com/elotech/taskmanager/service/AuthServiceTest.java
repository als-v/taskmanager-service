package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.error.ApiException;
import com.elotech.taskmanager.domain.error.ConflictException;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.domain.error.UnauthorizedException;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.repository.UserRepository;
import com.elotech.taskmanager.security.JwtService;
import com.elotech.taskmanager.security.RefreshTokenStore;
import com.elotech.taskmanager.domain.dto.response.auth.AuthResponse;
import com.elotech.taskmanager.domain.dto.request.auth.LoginRequest;
import com.elotech.taskmanager.domain.dto.request.auth.RefreshRequest;
import com.elotech.taskmanager.domain.dto.request.auth.SignUpRequest;
import com.elotech.taskmanager.domain.dto.response.auth.UserResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService, refreshTokenStore);
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Maria Silva")
                .email("maria@exemplo.com")
                .password("hashed")
                .build();
    }

    private Jws<Claims> mockRefreshClaims(String jti, String email) {
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn(jti);
        when(claims.getSubject()).thenReturn(email);
        @SuppressWarnings("unchecked")
        Jws<Claims> jws = mock(Jws.class);
        when(jws.getPayload()).thenReturn(claims);
        return jws;
    }

    @Test
    void signUp_shouldCreateUser_whenEmailNotInUse() {
        SignUpRequest request = new SignUpRequest("Maria Silva", "maria@exemplo.com", "senha123");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = authService.signUp(request);

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.name()).isEqualTo(user.getName());
        assertThat(response.email()).isEqualTo(user.getEmail());
    }

    @Test
    void signUp_shouldThrowConflict_whenEmailAlreadyInUse() {
        SignUpRequest request = new SignUpRequest("Maria Silva", "maria@exemplo.com", "senha123");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(ConflictException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorMessages.AUTH_EMAIL_IN_USE_CODE);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldReturnAuthResponseAndPersistRefreshToken_whenCredentialsValid() {
        LoginRequest request = new LoginRequest(user.getEmail(), "senha123");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.extractJtiFromRefreshToken("refresh-token")).thenReturn("jti-1");
        when(jwtService.getRefreshExpirationMs()).thenReturn(86_400_000L);

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo(user.getEmail());
        verify(refreshTokenStore).save("jti-1", user.getId(), Duration.ofMillis(86_400_000L));
    }

    @Test
    void login_shouldThrowUnauthorized_whenAuthenticationFails() {
        LoginRequest request = new LoginRequest(user.getEmail(), "wrong-password");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorMessages.AUTH_INVALID_CREDENTIALS_CODE);

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_shouldThrowUnauthorized_whenUserNotFoundAfterAuthentication() {
        LoginRequest request = new LoginRequest(user.getEmail(), "senha123");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorMessages.AUTH_INVALID_CREDENTIALS_CODE);
    }

    @Test
    void refresh_shouldRotateTokens_andDeleteOldJtiAfterSavingNewOne() {
        String oldJti = "old-jti";
        RefreshRequest request = new RefreshRequest("old-refresh-token");
        Jws<Claims> refreshClaims = mockRefreshClaims(oldJti, user.getEmail());
        when(jwtService.parseRefreshToken("old-refresh-token")).thenReturn(refreshClaims);
        when(refreshTokenStore.exists(oldJti)).thenReturn(true);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");
        when(jwtService.extractJtiFromRefreshToken("new-refresh-token")).thenReturn("new-jti");
        when(jwtService.getRefreshExpirationMs()).thenReturn(86_400_000L);

        AuthResponse response = authService.refresh(request);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        InOrder order = inOrder(refreshTokenStore);
        order.verify(refreshTokenStore).save(eq("new-jti"), eq(user.getId()), any(Duration.class));
        order.verify(refreshTokenStore).delete(oldJti);
    }

    @Test
    void refresh_shouldThrowRefreshExpired_whenTokenExpired() {
        RefreshRequest request = new RefreshRequest("expired-token");
        when(jwtService.parseRefreshToken("expired-token"))
                .thenThrow(new ExpiredJwtException(null, Jwts.claims().build(), "expired"));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorMessages.AUTH_REFRESH_EXPIRED_CODE);
    }

    @Test
    void refresh_shouldThrowRefreshInvalid_whenTokenMalformed() {
        RefreshRequest request = new RefreshRequest("malformed-token");
        when(jwtService.parseRefreshToken("malformed-token")).thenThrow(new JwtException("malformed"));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorMessages.AUTH_REFRESH_INVALID_CODE);
    }

    @Test
    void refresh_shouldThrowRefreshRevoked_whenJtiNotInStore() {
        RefreshRequest request = new RefreshRequest("revoked-token");
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn("revoked-jti");
        @SuppressWarnings("unchecked")
        Jws<Claims> revokedClaims = mock(Jws.class);
        when(revokedClaims.getPayload()).thenReturn(claims);
        when(jwtService.parseRefreshToken("revoked-token")).thenReturn(revokedClaims);
        when(refreshTokenStore.exists("revoked-jti")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorMessages.AUTH_REFRESH_REVOKED_CODE);

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void refresh_shouldThrowRefreshInvalid_whenUserNotFound() {
        RefreshRequest request = new RefreshRequest("orphan-token");
        Jws<Claims> orphanClaims = mockRefreshClaims("orphan-jti", "ghost@exemplo.com");
        when(jwtService.parseRefreshToken("orphan-token")).thenReturn(orphanClaims);
        when(refreshTokenStore.exists("orphan-jti")).thenReturn(true);
        when(userRepository.findByEmail("ghost@exemplo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorMessages.AUTH_REFRESH_INVALID_CODE);
    }

    @Test
    void logout_shouldDeleteJti_whenTokenValid() {
        RefreshRequest request = new RefreshRequest("valid-token");
        when(jwtService.extractJtiFromRefreshToken("valid-token")).thenReturn("jti-1");

        authService.logout(request);

        verify(refreshTokenStore).delete("jti-1");
    }

    @Test
    void logout_shouldNotThrow_whenTokenInvalid() {
        RefreshRequest request = new RefreshRequest("garbage-token");
        when(jwtService.extractJtiFromRefreshToken("garbage-token")).thenThrow(new JwtException("bad"));

        assertThatCode(() -> authService.logout(request)).doesNotThrowAnyException();
        verify(refreshTokenStore, never()).delete(anyString());
    }
}
