package com.forum.kma.postservice.service;

import com.forum.kma.common.exception.AppException;
import com.forum.kma.common.exception.CommonErrorCode;
import com.forum.kma.postservice.dto.AuthorizationInput;
import com.forum.kma.postservice.dto.UpdatePostRequest;
import com.forum.kma.postservice.exception.PostErrorCode;
import com.forum.kma.postservice.model.Post;
import com.forum.kma.postservice.repository.PostRepository;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostService {
    private final PostRepository postRepository;
    private final WebClient aclClient;

    public PostService(PostRepository postRepository, WebClient.Builder webClientBuilder) {
        this.postRepository = postRepository;
        this.aclClient = webClientBuilder.baseUrl("http://acl-service").build(); // Trỏ đến ACL Service
    }

    public Mono<Post> updatePost(String postId, UpdatePostRequest request) {
        return postRepository.findById(postId)
                .flatMap(existingPost -> {
                    // cập nhật nếu có giá trị mới
                    if (request.newTitle() != null && !request.newTitle().isBlank()) {
                        existingPost.setTitle(request.newTitle());
                    }
                    if (request.newContent() != null && !request.newContent().isBlank()) {
                        existingPost.setContent(request.newContent());
                    }
                    return postRepository.save(existingPost);
                })
                .switchIfEmpty(Mono.error(new AppException(PostErrorCode.RESOURCE_NOT_FOUND)));
    }

    public Mono<Post> createPost(String authorId, com.forum.kma.postservice.dto.CreatePostRequest req) {
        Post post = Post.builder()
                .title(req.title())
                .content(req.content())
                .authorId(authorId)
                .status(Post.Status.DRAFT)
                .reactionCount(0)
                .build();

        return postRepository.save(post).map(Post::asNotNew);
    }

    public Mono<Post> getPostById(String postId) {
        return postRepository.findById(postId);
    }

    public Mono<Void> deletePostById(String postId) {
        return postRepository.deleteById(postId);
    }

    public Flux<Post> listByAuthor(String authorId, String search) {
        return postRepository.findByAuthorIdAndTitleContaining(authorId, search);
    }

    /**
     * Logic chính: Cập nhật bài đăng (Tuân thủ OPA Flow)
     */
//    public Mono<Post> updatePost1(String postId, UpdatePostRequest request) {
//
//        // 1. Lấy thông tin User từ SecurityContext (do SecurityConfig cung cấp)
//        Mono<Authentication> authMono = ReactiveSecurityContextHolder.getContext()
//                .map(SecurityContext::getAuthentication)
//                .switchIfEmpty(Mono.error(new AppException(CommonErrorCode.UNAUTHORIZED)));
//
//        // 2. Lấy thông tin tài nguyên (Post) từ DB (Bước C)
//        Mono<Post> postMono = postRepository.findById(postId)
//                .switchIfEmpty(Mono.error(new AppException(PostErrorCode.RESOURCE_NOT_FOUND)));
//
//        // 3. Kết hợp cả hai (User + Resource)
//        return Mono.zip(authMono, postMono)
//                .flatMap(tuple -> {
//                    Authentication auth = tuple.getT1();
//                    Post post = tuple.getT2();
//
//                    String userId = auth.getName(); // Lấy principal (userId)
//                    // Lấy vai trò (giả sử chỉ có 1 vai trò)
//                    String roleId = auth.getAuthorities().stream()
//                            .findFirst()
//                            .map(GrantedAuthority::getAuthority)
//                            .orElse("ROLE_DEFAULT")
//                            .replace("ROLE_", ""); // Bỏ tiền tố ROLE_
//
//                    // 4. Chuẩn bị Input cho ACL (Bước D)
//                    AuthorizationInput aclInput = new AuthorizationInput(
//                            userId,
//                            roleId,
//                            "edit", // action
//                            "post", // resourceType
//                            post.getPostId(),
//                            post.getAuthorId(),
//                            post.getStatus().toString()
//                    );
//
//                    // 5. Gọi ACL Service (Bước E)
//                    return callAclService(aclInput)
//                            .flatMap(decision -> {
//                                // 6. Xử lý quyết định (Bước F)
//                                if (decision.isGranted()) {
//                                    // Được phép -> Thực thi logic nghiệp vụ
//                                    Post updatedPost = new Post(
//                                            post.getPostId(), post.getAuthorId(),
//                                            request.newTitle(), // Cập nhật title
//                                            request.newContent(), // Cập nhật content
//                                            post.getStatus(),
//                                            post.getReactionCount()
//                                    );
//                                    // Lưu vào DB
//                                    return postRepository.save(updatedPost);
//                                } else {
//                                    // Không được phép
//                                    return Mono.error(new AppException(CommonErrorCode.UNAUTHORIZED));
//                                }
//                            });
//                });
//    }

    /**
     * Hàm riêng để gọi ACL Service
     */
    private Mono<AuthorizationDecision> callAclService(AuthorizationInput input) {
        System.out.println("Gửi yêu cầu phân quyền đến ACL: " + input);
        return aclClient.post()
                .uri("/v1/authorize")
                .bodyValue(input)
                .retrieve()
                // Xử lý nếu ACL service bị lỗi
                .onStatus(status -> status.isError(), clientResponse ->
                        Mono.error(new RuntimeException("ACL Service unavailable."))
                )
                .bodyToMono(AuthorizationDecision.class)
                .switchIfEmpty(Mono.just(new AuthorizationDecision(false))); // Mặc định là từ chối nếu ACL trả về rỗng
    }
}
