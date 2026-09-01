package com.elotech.taskmanager.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<String, StoredRefreshToken> tokens = new ConcurrentHashMap<>();

    @Override
    public void save(String jti, UUID userId, Duration ttl) {
        tokens.put(jti, new StoredRefreshToken(userId, Instant.now().plus(ttl)));
    }

    @Override
    public boolean exists(String jti) {
        StoredRefreshToken token = tokens.get(jti);

        if (token == null) {
            return false;
        }

        if (token.expiresAt().isBefore(Instant.now())) {
            tokens.remove(jti);
            return false;
        }

        return true;
    }

    @Override
    public void delete(String jti) {
        tokens.remove(jti);
    }

    private record StoredRefreshToken(UUID userId, Instant expiresAt) {
    }
}
