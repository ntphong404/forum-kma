package com.forum.kma.postservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.WebFilter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String SESSION_ID_HEADER = "X-Session-Id";
    private static final String REDIS_SESSION_KEY_PREFIX = "PERM:";

    // Cache tạm thời các quyền để giảm tải Redis
    private final ConcurrentHashMap<String, List<GrantedAuthority>> cache = new ConcurrentHashMap<>();

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ReactiveStringRedisTemplate redisTemplate) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .addFilterAt(gatewayHeaderAuthenticationFilter(redisTemplate), SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .build();
    }

    @Bean
    public WebFilter gatewayHeaderAuthenticationFilter(ReactiveStringRedisTemplate redisTemplate) {
        return (exchange, chain) -> {
            String userId = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
            String sessionId = exchange.getRequest().getHeaders().getFirst(SESSION_ID_HEADER);
            String uri = exchange.getRequest().getURI().toString();

            if (userId == null || sessionId == null) {
                log.warn("Missing authentication headers for {}. Responding with 401.", uri);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String redisKey = REDIS_SESSION_KEY_PREFIX + sessionId;

            // 1️⃣ Kiểm tra cache
            List<GrantedAuthority> cached = cache.get(redisKey);
            if (cached != null) {
                log.debug("Cache hit for session {} on {}", sessionId, uri);
                Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, cached);
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            }

            // 2️⃣ Nếu không có cache, lấy từ Redis (Set)
            return redisTemplate.opsForSet().members(redisKey)
                    .collectList()
                    .flatMap(permissions -> {
                        if (permissions.isEmpty()) {
                            log.warn("No permissions found in Redis for {} ({})", userId, sessionId);
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }

                        List<GrantedAuthority> authorities = permissions.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                        cache.put(redisKey, authorities);
                        log.info("Loaded permissions for user {} ({}): {}", userId, sessionId, permissions);

                        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                    })
                    .onErrorResume(e -> {
                        log.error("Redis error while reading permissions [{}]: {}", redisKey, e.getMessage());
                        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        return exchange.getResponse().setComplete();
                    });
        };
    }
}
