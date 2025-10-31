package com.forum.kma.authservice.config;

import com.forum.kma.authservice.model.Role;
import com.forum.kma.authservice.model.User;
import com.forum.kma.authservice.repository.RoleRepository;
import com.forum.kma.authservice.repository.UserRepository;
import com.forum.kma.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class JwtAuthenticationWebFilter implements WebFilter {
    private final JwtUtil jwtUtil;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        // Bắt đầu chuỗi Reactive:
        Mono<Authentication> authenticationMono = Mono.fromCallable(() -> jwtUtil.validateToken(token))
                .doOnError(e -> log.error("JWT Validation failed for token: {}. Error: {}", token, e.getMessage()))
                .onErrorResume(Exception.class, e -> Mono.empty())

                // Tải User và Role (RBAC Logic)
                .flatMap(claims -> {
                    String userId = claims.userId();
                    String roleId = claims.roleId();

                    if (userId == null || roleId == null) return Mono.empty();

                    // Tải User và Role/Permissions song song
                    return Mono.zip(
                                    userRepository.findById(userId),
                                    roleRepository.findById(roleId)
                            )
                            .map(tuple -> {
                                User userPrincipal = tuple.getT1();
                                Role role = tuple.getT2();

                                // TẠO AUTHORITIES CHỈ TỪ PERMISSIONS
                                List<GrantedAuthority> authorities = role.getPermissions().stream()
                                        .map(SimpleGrantedAuthority::new)
                                        .collect(Collectors.toList());

                                // Đặt Custom User Entity làm Principal
                                return new UsernamePasswordAuthenticationToken(
                                        userPrincipal,
                                        null,
                                        authorities
                                );
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                log.error("Authentication failed:(User or Role missing)");
                                return Mono.empty();
                            }));
                });

        // Đặt Authentication vào Security Context
        return authenticationMono
                .flatMap(auth -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(auth)))))
                .switchIfEmpty(chain.filter(exchange));
    }
}