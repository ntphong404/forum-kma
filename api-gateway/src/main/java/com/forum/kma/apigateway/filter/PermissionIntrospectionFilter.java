//package com.forum.kma.apigateway.filter;
//
//import com.forum.kma.common.security.JwtUtil;
//import com.forum.kma.common.security.JwtClaims;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.annotation.Order;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.server.ResponseStatusException;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.util.Collections;
//import java.util.Set;
//
//import org.springframework.core.ParameterizedTypeReference;
//
//@Component
//@Order(-100)
//@RequiredArgsConstructor
//@Slf4j
//public class PermissionIntrospectionFilter implements GlobalFilter {
//
//    private final JwtUtil jwtUtil;
//    private final WebClient webClient;
//
//    @Value("${internal.gateway.secret-key}")
//    private String internalSecret;
//
//    private static final String PERMISSIONS_HEADER = "X-Permissions";
//    private static final String USER_ID_HEADER = "X-User-ID";
//
//    // Public endpoint
//    private boolean shouldSkip(ServerWebExchange exchange) {
//        String path = exchange.getRequest().getPath().value();
//        // Kiểm tra xem đường dẫn có bắt đầu bằng /api/auth hay không
//        // (Ví dụ: /api/auth/login, /api/auth/register)
//        return path.startsWith("/api/auth");
//    }
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//
//        // 1. ÁP DỤNG LOGIC SHOULD SKIP
//        if (shouldSkip(exchange)) {
//            return chain.filter(exchange);
//        }
//
//        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//
//        // 2. XỬ LÝ KHÔNG CÓ TOKEN (Tức là API là protected, nhưng không có token)
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
//        }
//
//        String token = authHeader.substring(7);
//
//        // 3. CHUYỂN LOGIC BLOCKING SANG REACTIVE VÀ TÍCH HỢP INTROSPECTION
//        Mono<JwtClaims> claimsMono = Mono.fromCallable(() -> jwtUtil.validateToken(token))
//                .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT.")));
//
//        return claimsMono
//                .flatMap(claims -> {
//                    if (!"access".equals(claims.type())) {
//                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is not an Access Token."));
//                    }
//
//                    // Gọi nội bộ Auth Service để lấy Permissions
//                    Mono<Set<String>> permissionsMono = webClient.get()
//                            .uri("lb://AUTH-SERVICE/api/internal/permissions/{roleId}", claims.roleId())
//                            .header("X-Internal-Secret", internalSecret)
//                            .retrieve()
//                            // Chỉ định kiểu trả về là Set<String>
//                            .bodyToMono(new ParameterizedTypeReference<Set<String>>() {})
//                            .onErrorResume(e -> Mono.just(Collections.emptySet()));
//
//                    // 4. ÁP DỤNG HEADERS VÀ CHUYỂN TIẾP
//                    return permissionsMono.flatMap(permissions -> {
//                        String permissionsString = String.join(",", permissions);
//                        log.info("Headers added: User={}, Permissions={}", claims.userId(), permissionsString);
//                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
//                                .header(USER_ID_HEADER, claims.userId())
//                                .header(PERMISSIONS_HEADER, permissionsString)
//                                .header("X-Internal-Secret", internalSecret)
//                                .build();
//
//                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
//                    });
//                })
//                // 5. XỬ LÝ LỖI TOÀN CỤC CHO CÁC LỖI REACTIVE (401, 403)
//                .onErrorResume(ResponseStatusException.class, e -> {
//                    exchange.getResponse().setStatusCode(e.getStatusCode());
//                    return exchange.getResponse().setComplete();
//                });
//    }
//
//    /* Lưu ý: Cấu hình Gateway phải định tuyến /api/auth/** tới Auth Service.
//     Nếu bạn dùng tiền tố /api/v1/, logic shouldSkip cần được điều chỉnh cho phù hợp.
//     */
//}