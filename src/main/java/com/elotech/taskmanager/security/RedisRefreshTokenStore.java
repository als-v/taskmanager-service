package com.elotech.taskmanager.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@Profile("!test")
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String jti, UUID userId, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, userId.toString(), ttl);
    }

    @Override
    public boolean exists(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }

    @Override
    public void delete(String jti) {
        redisTemplate.delete(KEY_PREFIX + jti);
    }
}
