package com.forum.kma.apigateway.config;

import com.forum.kma.common.security.JwtProperties;
import com.forum.kma.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtUtil jwtUtil(@Qualifier("jwt-com.forum.kma.common.security.JwtProperties") JwtProperties props) {
        return new JwtUtil(props);
    }
}