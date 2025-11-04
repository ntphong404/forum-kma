package com.forum.kma.postservice.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.kma.common.dto.response.ApiResponse;
import com.forum.kma.postservice.dto.AclCheckRequest;
import com.forum.kma.postservice.dto.AclCheckResponse;
import com.forum.kma.postservice.repository.PostRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class AclWebFilter implements WebFilter {

    private final WebClient.Builder webClientBuilder;
    private final PostRepository postRepository;
    private final ObjectMapper objectMapper;

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getPath().value();
        String method = req.getMethod().name();

        // ❌ Bỏ qua nếu không phải endpoint liên quan đến bài viết
        // ✅ Lấy postId từ URL (VD: /api/posts/123 → "123")
        String[] segments = path.split("/");
        log.info("{}", Arrays.toString(segments));
        if (segments.length != 4 || !segments[1].equals("api") || !segments[2].equals("posts")) {
            // Loại trừ các trường hợp: /api/posts/author/123 (length=5), /api/posts (length=3), /api/users (segments[2] != "posts")
            return chain.filter(exchange);
        }

        String postId = segments[3];

        // ✅ Chuyển HTTP method → action logic
        String action = switch (method) {
            case "GET" -> "READ";
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "UNKNOWN";
        };
        log.info("{} {} {} {}", method, path, action, postId);
        // ✅ Lấy thông tin user từ SecurityContext (đã có từ Gateway)
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> {
                    var auth = ctx.getAuthentication();
                    String userId = (String) auth.getPrincipal();
                    String roleName = auth.getAuthorities().stream()
                            .map(a -> a.getAuthority().replaceAll("[\\[\\]\"]", "").trim())
                            .filter(a -> a.startsWith("ROLE_"))
                            .findFirst()
                            .orElse("NO_ROLE");

                    log.info("{}", roleName);
                    // 🧩 Lấy authorId thật từ DB
                    return postRepository.findById(postId)
                            .switchIfEmpty(Mono.error(new RuntimeException("Post not found")))
                            .flatMap(post -> {
                                String authorId = post.getAuthorId();

                                AclCheckRequest body = new AclCheckRequest(
                                        "POST",
                                        postId,
                                        authorId,
                                        userId,
                                        action,
                                        roleName
                                );

                                return webClientBuilder.build()
                                        .post()
                                        .uri("lb://acl-service/api/acl/check")
                                        .bodyValue(body)
                                        .retrieve()
                                        .bodyToMono(AclCheckResponse.class)
                                        .flatMap(res -> {
                                            if (res.isAllowed()) {
                                                return chain.filter(exchange);
                                            } else {
                                                return handleApiError(exchange, "You don't have permission", HttpStatus.FORBIDDEN);
                                            }
                                        });
                            })
                            .onErrorResume(e -> handleApiError(exchange, e.getMessage(), HttpStatus.NOT_FOUND));
                })
                .onErrorResume(e -> {
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });
    }

    private Mono<Void> handleApiError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<?> apiResponse = ApiResponse.error(status.value(), message);

        DataBufferFactory bufferFactory = response.bufferFactory();

        try {
            // Chuyển đối tượng ApiResponse thành chuỗi JSON
            byte[] bytes = objectMapper.writeValueAsBytes(apiResponse);

            // Ghi JSON vào DataBuffer
            DataBuffer buffer = bufferFactory.wrap(bytes);

            // Ghi buffer vào response và hoàn tất
            return response.writeWith(Mono.just(buffer));

        } catch (JsonProcessingException e) {
            log.error("Error serializing ApiResponse: {}", e.getMessage());
            // Trả về phản hồi 401 mặc định nếu serialization thất bại
            return response.setComplete();
        }
    }
}
