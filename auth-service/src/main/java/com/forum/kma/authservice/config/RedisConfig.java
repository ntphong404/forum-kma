package com.forum.kma.authservice.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    @Primary
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        ObjectMapper redisMapper = new ObjectMapper();
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.forum.kma.") // chỉ cho phép package hợp lệ
                .build();

        redisMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        Jackson2JsonRedisSerializer<Object> valueSerializer =
                new Jackson2JsonRedisSerializer<>(redisMapper, Object.class);

        RedisSerializationContext<String, Object> context =
                RedisSerializationContext.<String, Object>newSerializationContext(keySerializer)
                        .hashKey(keySerializer)
                        .value(valueSerializer)
                        .hashValue(valueSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}
