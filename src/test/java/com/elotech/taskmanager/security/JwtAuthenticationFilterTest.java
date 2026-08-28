package com.elotech.taskmanager.security;

import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, objectMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    private Jws<Claims> mockAccessClaims(String email) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        Jws<Claims> jws = mock(Jws.class);
        when(jws.getPayload()).thenReturn(claims);
        return jws;
    }

    @Test
    void doFilterInternal_shouldContinueWithoutAuthentication_whenNoAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldSetAuthentication_whenAccessTokenValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Jws<Claims> accessClaims = mockAccessClaims("maria@exemplo.com");
        when(jwtService.validateAccessToken("valid-access-token")).thenReturn(accessClaims);
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                "maria@exemplo.com", "hash", java.util.Collections.emptyList());
        when(userDetailsService.loadUserByUsername("maria@exemplo.com")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("maria@exemplo.com");
    }

    @Test
    void doFilterInternal_shouldReturn401WithTokenExpiredCode_whenTokenExpired() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validateAccessToken("expired-token"))
                .thenThrow(new ExpiredJwtException(null, Jwts.claims().build(), "expired"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, org.mockito.Mockito.never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo(ErrorMessages.AUTH_TOKEN_EXPIRED_CODE);
    }

    @Test
    void doFilterInternal_shouldReturn401WithTokenInvalidCode_whenTokenMalformed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer malformed-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validateAccessToken("malformed-token")).thenThrow(new JwtException("malformed"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, org.mockito.Mockito.never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo(ErrorMessages.AUTH_TOKEN_INVALID_CODE);
    }
}
