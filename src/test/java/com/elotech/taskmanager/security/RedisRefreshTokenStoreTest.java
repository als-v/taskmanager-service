package com.elotech.taskmanager.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisRefreshTokenStore store;

    @Test
    void save_shouldStoreUserIdUnderPrefixedKeyWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID userId = UUID.randomUUID();
        Duration ttl = Duration.ofMinutes(5);

        store.save("jti-1", userId, ttl);

        verify(valueOperations).set("refresh:jti-1", userId.toString(), ttl);
    }

    @Test
    void exists_shouldReturnTrue_whenKeyPresent() {
        when(redisTemplate.hasKey("refresh:jti-1")).thenReturn(true);

        assertThat(store.exists("jti-1")).isTrue();
    }

    @Test
    void exists_shouldReturnFalse_whenKeyAbsent() {
        when(redisTemplate.hasKey("refresh:jti-1")).thenReturn(false);

        assertThat(store.exists("jti-1")).isFalse();
    }

    @Test
    void delete_shouldRemoveKeyWithPrefix() {
        store.delete("jti-1");

        verify(redisTemplate).delete("refresh:jti-1");
    }
}
