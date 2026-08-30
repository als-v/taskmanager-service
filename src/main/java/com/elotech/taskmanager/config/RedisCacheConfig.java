package com.elotech.taskmanager.config;

import com.elotech.taskmanager.service.CacheInvalidationService;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisCacheConfig {

    static final String CACHE_KEY_PREFIX = "tm:v2:";

    @Bean
    CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration defaultConfiguration = cacheConfiguration(objectMapper, Duration.ofMinutes(2));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(Map.of(
                        CacheInvalidationService.DASHBOARD_SUMMARY_CACHE,
                        cacheConfiguration(objectMapper, Duration.ofMinutes(5)),
                        CacheInvalidationService.DASHBOARD_WIP_CACHE,
                        cacheConfiguration(objectMapper, Duration.ofMinutes(2)),
                        CacheInvalidationService.PROJECT_MEMBERS_LIST_CACHE,
                        cacheConfiguration(objectMapper, Duration.ofMinutes(5)),
                        CacheInvalidationService.USER_PROJECTS_LIST_CACHE,
                        cacheConfiguration(objectMapper, Duration.ofMinutes(2))
                ))
                .build();
    }

    RedisCacheConfiguration cacheConfiguration(ObjectMapper objectMapper, Duration ttl) {
        ObjectMapper cacheObjectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule());

        cacheObjectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer.registerNullValueSerializer(cacheObjectMapper, null);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> CACHE_KEY_PREFIX + cacheName + "::")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}
