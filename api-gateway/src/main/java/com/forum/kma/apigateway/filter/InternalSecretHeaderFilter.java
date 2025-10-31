//package com.forum.kma.apigateway.filter;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//import org.springframework.core.annotation.Order;
//
//@Component
//@Order(Ordered.HIGHEST_PRECEDENCE) // Đảm bảo chạy RẤT SỚM
//public class InternalSecretHeaderFilter implements GlobalFilter, Ordered {
//
//    @Value("${internal.gateway.secret-key}")
//    private String internalSecret;
//
//    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        // Lấy request gốc và thêm X-Internal-Secret vào đó
//        ServerHttpRequest request = exchange.getRequest().mutate()
//                .header(INTERNAL_SECRET_HEADER, internalSecret)
//                .build();
//
//        // Chuyển tiếp request đã được chỉnh sửa
//        return chain.filter(exchange.mutate().request(request).build());
//    }
//
//    @Override
//    public int getOrder() {
//        return -200; // Đặt thứ tự cao hơn cả PermissionIntrospectionFilter (-100)
//    }
//}