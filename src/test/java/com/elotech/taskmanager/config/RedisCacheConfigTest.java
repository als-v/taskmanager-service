package com.elotech.taskmanager.config;

import com.elotech.taskmanager.domain.dto.response.audit.AuditLogReferenceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCacheConfigTest {

    @Test
    void configuresVersionedPrefixAndTypeMetadataForFinalRecords() {
        RedisCacheConfig config = new RedisCacheConfig();
        RedisCacheConfiguration cacheConfiguration = config.cacheConfiguration(new ObjectMapper(), Duration.ofMinutes(2));
        AuditLogReferenceResponse value = new AuditLogReferenceResponse(UUID.randomUUID(), "Criar endpoint de status");

        ByteBuffer serialized = cacheConfiguration.getValueSerializationPair().write(value);
        String json = StandardCharsets.UTF_8.decode(serialized).toString();

        assertThat(cacheConfiguration.getKeyPrefixFor("reports")).isEqualTo("tm:v2:reports::");
        assertThat(json).contains("@class");
        assertThat(json).contains(AuditLogReferenceResponse.class.getName());
    }
}
