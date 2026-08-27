package com.elotech.taskmanager.security;

import java.time.Duration;
import java.util.UUID;

public interface RefreshTokenStore {

    void save(String jti, UUID userId, Duration ttl);

    boolean exists(String jti);

    void delete(String jti);
}
