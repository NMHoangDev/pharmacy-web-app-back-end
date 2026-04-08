package com.backend.branch.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    private final RedisProperties redisProperties;

    public RedisConfig(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }

    @Bean(name = "redisObjectTemplate")
    @Primary
    public RedisTemplate<String, Object> redisObjectTemplate(
            RedisConnectionFactory connectionFactory) {
        // Read endpoint from spring.data.redis.* in application.yml to avoid hardcoded
        // values and keep the same config reusable for every service.
        if (redisProperties.getHost() == null || redisProperties.getPort() == 0) {
            throw new IllegalStateException(
                    "Missing Redis configuration: spring.data.redis.host and spring.data.redis.port");
        }

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // String keys keep cache entries consistent and readable across operations.
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        // JSON values allow storing DTOs, Lists, and custom objects without manual
        // serialization code.
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
