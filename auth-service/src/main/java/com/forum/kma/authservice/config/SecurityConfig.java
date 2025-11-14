package com.forum.kma.authservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;

import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationWebFilter jwtAuthenticationWebFilter;
    private final ServerAuthenticationEntryPoint jwtServerAuthenticationEntryPoint; // Sử dụng Interface

    // Sử dụng Constructor Injection để tiêm các dependency một cách rõ ràng
    public SecurityConfig(
            JwtAuthenticationWebFilter jwtAuthenticationWebFilter,
            @Qualifier("customJwtAuthenticationEntryPoint") ServerAuthenticationEntryPoint jwtServerAuthenticationEntryPoint // Tiêm qua Interface
    ) {
        this.jwtAuthenticationWebFilter = jwtAuthenticationWebFilter;
        this.jwtServerAuthenticationEntryPoint = jwtServerAuthenticationEntryPoint;
    }

    private final String[] PUBLIC_ENDPOINTS = {
            "/auth/login",
            "/auth/login/verify",
            "/auth/register",
            "/auth/refresh",
            "/auth/reset-password",
            "/auth/forgot-password",
            "/auth/verify-otp",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/internal/**"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyExchange().authenticated())
                .addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(e -> e.authenticationEntryPoint(jwtServerAuthenticationEntryPoint))
                .build();
    }

}
